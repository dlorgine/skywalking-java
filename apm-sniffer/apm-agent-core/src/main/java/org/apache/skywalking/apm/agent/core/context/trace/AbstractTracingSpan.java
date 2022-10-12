/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.context.trace;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.internal.DefaultTransaction;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.cat.CatContext;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.TracingContext;
import org.apache.skywalking.apm.agent.core.context.status.StatusCheckService;
import org.apache.skywalking.apm.agent.core.context.tag.AbstractTag;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.util.KeyValuePair;
import org.apache.skywalking.apm.agent.core.context.util.TagValuePair;
import org.apache.skywalking.apm.agent.core.context.util.ThrowableTransformer;
import org.apache.skywalking.apm.agent.core.dictionary.DictionaryUtil;
import org.apache.skywalking.apm.network.language.agent.v3.SpanObject;
import org.apache.skywalking.apm.network.language.agent.v3.SpanType;
import org.apache.skywalking.apm.network.trace.component.Component;
import org.apache.skywalking.apm.network.trace.component.OfficialComponent;
import org.apache.skywalking.apm.util.StringUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * The <code>AbstractTracingSpan</code> represents a group of {@link AbstractSpan} implementations, which belongs a real
 * distributed trace.
 */
public abstract class AbstractTracingSpan implements AbstractSpan {
    /**
     * Span id starts from 0.
     */
    protected int spanId;
    /**
     * Parent span id starts from 0. -1 means no parent span.
     */
    protected int parentSpanId;
    protected List<TagValuePair> tags;
    protected String operationName;
    protected SpanLayer layer;
    /**
     * The span has been tagged in async mode, required async stop to finish.
     */
    protected volatile boolean isInAsyncMode = false;
    /**
     * The flag represents whether the span has been async stopped
     */
    private volatile boolean isAsyncStopped = false;

    /**
     * The context to which the span belongs
     */
    protected final TracingContext owner;

    /**
     * The start time of this Span.
     */
    protected long startTime;
    /**
     * The end time of this Span.
     */
    protected long endTime;
    /**
     * Error has occurred in the scope of span.
     */
    protected boolean errorOccurred = false;

    protected int componentId = 0;

    /**
     * Log is a concept from OpenTracing spec. https://github.com/opentracing/specification/blob/master/specification.md#log-structured-data
     */
    protected List<LogDataEntity> logs;

    /**
     * The refs of parent trace segments, except the primary one. For most RPC call, {@link #refs} contains only one
     * element, but if this segment is a start span of batch process, the segment faces multi parents, at this moment,
     * we use this {@link #refs} to link them.
     */
    protected List<TraceSegmentRef> refs;

    protected com.dianping.cat.message.Transaction transcation;
    /**
     * Tracing Mode. If true means represents all spans generated in this context should skip analysis.
     */
    protected boolean skipAnalysis;

    protected AbstractTracingSpan(int spanId, int parentSpanId, String operationName, TracingContext owner) {
        this.operationName = operationName;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.owner = owner;
    }

    /**
     * Set a key:value tag on the Span.
     * <p>
     * {@inheritDoc}
     *
     * @return this Span instance, for chaining
     */
    @Override
    public AbstractTracingSpan tag(String key, String value) {
        return tag(Tags.ofKey(key), value);
    }

    @Override
    public AbstractTracingSpan tag(AbstractTag<?> tag, String value) {
        if (tags == null) {
            tags = new ArrayList<>(8);
        }

        if (tag.isCanOverwrite()) {
            for (TagValuePair pair : tags) {
                if (pair.sameWith(tag)) {
                    pair.setValue(value);
                    return this;
                }
            }
        }

        tags.add(new TagValuePair(tag, value));
        return this;
    }

    /**
     * Finish the active Span. When it is finished, it will be archived by the given {@link TraceSegment}, which owners
     * it.
     *
     * @param owner of the Span.
     */
    public boolean finish(TraceSegment owner) {
        this.endTime = System.currentTimeMillis();
        owner.archive(this);
        if (transcation != null) {
            if (!errorOccurred) {
                transcation.setStatus(Transaction.SUCCESS);
            }
            if (transcation instanceof DefaultTransaction) {
                DefaultTransaction defaultTransaction = (DefaultTransaction) transcation;
                defaultTransaction.setType(getSpanType().name() + ":" + OfficialComponent.getName(componentId));
                UrlSchema urlSchema = getSchema();
                if (StringUtil.isNotEmpty(urlSchema.schema)) {
                    if (isEntry()) {
                        defaultTransaction.setName(urlSchema.schema);
                    } else if (isExit()) {
                        defaultTransaction.setName(getExitName(urlSchema));
                        defaultTransaction.addData("orign-url", urlSchema.url);

                    }
                } else if (isExit()) {
                    defaultTransaction.setName(getExitName(urlSchema));
                    if (!urlSchema.flag) {
                        defaultTransaction.addData("orign-url", urlSchema.url);
                    }
                } else {
                    defaultTransaction.setName(urlSchema.url);
                }

            }
            transcation.complete();
        }
        return true;
    }

    private String getExitName(UrlSchema urlSchema) {
        try {
            if (StringUtil.isNotEmpty(urlSchema.url) && urlSchema.url.startsWith("http")) {
                if (StringUtil.isNotEmpty(urlSchema.schema)) {
                    if (StringUtil.isEmpty(urlSchema.domain)) {
                        return urlSchema.schema;
                    } else {
                        String url = urlSchema.url.substring(0, urlSchema.url.indexOf('/', 10)) + urlSchema.schema;
                        return url + "(" + urlSchema.domain + ")";
                    }
                }
            }
        } catch (Throwable e) {
        }
        urlSchema.flag = true;
        return urlSchema.url;
    }

    public static class UrlSchema {
        public String url;
        public String schema;
        public String domain;
        public boolean flag = false;
    }

    private UrlSchema getSchema() {
        UrlSchema urlSchema = new UrlSchema();
        String url = "";
        String method = "";
        String parrtern = "";
        for (TagValuePair keyStringValuePair : tags) {
            if (Tags.URL.key().equalsIgnoreCase(keyStringValuePair.getKey().key())) {
                url = keyStringValuePair.getValue();
            } else if (Tags.HTTP.METHOD.key().equalsIgnoreCase(keyStringValuePair.getKey().key())) {
                method = keyStringValuePair.getValue();
            } else if (Tags.URL_SCHEMA.key().equalsIgnoreCase(keyStringValuePair.getKey().key())) {
                parrtern = keyStringValuePair.getValue();
            } else if (Tags.DOMAIN_NAME.key().equalsIgnoreCase(keyStringValuePair.getKey().key())) {
                urlSchema.domain = keyStringValuePair.getValue();
            }
        }
        if (StringUtil.isNotEmpty(parrtern)) {
            if (parrtern.indexOf("?") != -1) {
                parrtern = parrtern.substring(0, parrtern.indexOf("?"));
            }
            urlSchema.schema = parrtern + ":" + method;
        }
        if (url.length() == 0) {
            urlSchema.url = getOperationName();
        } else {
            if (isEntry()) {
                try {
                    URL url1 = new URL(url);
                    urlSchema.url = url1.getPath() + ":" + method;
                    if (StringUtil.isEmpty(urlSchema.schema) && url.contains("?")) {
                        urlSchema.schema = url1.getPath().substring(0, url1.getPath().indexOf("?")) + ":" + method;
                    }
                } catch (Throwable e) {
                    urlSchema.url = url + ":" + method;
                    if (StringUtil.isEmpty(urlSchema.schema) && url.contains("?")) {
                        urlSchema.schema = url.substring(0, url.indexOf("?")) + ":" + method;
                    }
                }
            } else {
                urlSchema.url = url + ":" + method;
                if (StringUtil.isEmpty(urlSchema.schema) && url.contains("?")) {
                    urlSchema.schema = url.substring(0, url.indexOf("?")) + ":" + method;
                }
            }
        }
        return urlSchema;
    }

    @Override
    public AbstractTracingSpan start() {
        this.startTime = System.currentTimeMillis();
        addCat();
        transcation = Cat.newTransaction(getOperationName(), "");
        return this;
    }

    private void addCat() {
        if (isExit()) {
            CatContext catContext = new CatContext();
            Cat.logRemoteCallClient(catContext, Cat.getManager().getDomain());
            ContextManager.getCorrelationContext().put(Cat.Context.ROOT, catContext.getProperty(Cat.Context.ROOT));
            ContextManager.getCorrelationContext().put(Cat.Context.PARENT, catContext.getProperty(Cat.Context.PARENT));
            ContextManager.getCorrelationContext().put(Cat.Context.CHILD, catContext.getProperty(Cat.Context.CHILD));
        }
    }


    /**
     * Record an exception event of the current walltime timestamp.
     *
     * @param t any subclass of {@link Throwable}, which occurs in this span.
     * @return the Span, for chaining
     */
    @Override
    public AbstractTracingSpan log(Throwable t) {
        if (logs == null) {
            logs = new LinkedList<>();
        }
        if (!errorOccurred && ServiceManager.INSTANCE.findService(StatusCheckService.class).isError(t)) {
            errorOccurred();
        }
        logs.add(new LogDataEntity.Builder().add(new KeyValuePair("event", "error"))
                .add(new KeyValuePair("error.kind", t.getClass().getName()))
                .add(new KeyValuePair("message", t.getMessage()))
                .add(new KeyValuePair(
                        "stack",
                        ThrowableTransformer.INSTANCE.convert2String(t, 4000)
                ))
                .build(System.currentTimeMillis()));
        transcation.setStatus(t);
        return this;
    }

    /**
     * Record a common log with multi fields, for supporting opentracing-java
     *
     * @return the Span, for chaining
     */
    @Override
    public AbstractTracingSpan log(long timestampMicroseconds, Map<String, ?> fields) {
        if (logs == null) {
            logs = new LinkedList<>();
        }
        LogDataEntity.Builder builder = new LogDataEntity.Builder();
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            builder.add(new KeyValuePair(entry.getKey(), entry.getValue().toString()));
        }
        logs.add(builder.build(timestampMicroseconds));
        return this;
    }

    /**
     * In the scope of this span tracing context, error occurred, in auto-instrumentation mechanism, almost means throw
     * an exception.
     *
     * @return span instance, for chaining.
     */
    @Override
    public AbstractTracingSpan errorOccurred() {
        this.errorOccurred = true;
        return this;
    }

    /**
     * Set the operation name, just because these is not compress dictionary value for this name. Use the entire string
     * temporarily, the agent will compress this name in async mode.
     *
     * @return span instance, for chaining.
     */
    @Override
    public AbstractTracingSpan setOperationName(String operationName) {
        this.operationName = operationName;
        return this;
    }

    @Override
    public int getSpanId() {
        return spanId;
    }

    @Override
    public String getOperationName() {
        return operationName;
    }

    @Override
    public AbstractTracingSpan setLayer(SpanLayer layer) {
        this.layer = layer;
        return this;
    }

    /**
     * Set the component of this span, with internal supported. Highly recommend to use this way.
     *
     * @return span instance, for chaining.
     */
    @Override
    public AbstractTracingSpan setComponent(Component component) {
        this.componentId = component.getId();
        return this;
    }

    @Override
    public AbstractSpan start(long startTime) {
        this.startTime = startTime;
        return this;
    }

    private SpanType getSpanType() {
        if (isEntry()) {
            return SpanType.Entry;
        } else if (isExit()) {
            return SpanType.Exit;
        } else {
            return SpanType.Local;
        }
    }

    public SpanObject.Builder transform() {
        SpanObject.Builder spanBuilder = SpanObject.newBuilder();

        spanBuilder.setSpanId(this.spanId);
        spanBuilder.setParentSpanId(parentSpanId);
        spanBuilder.setStartTime(startTime);
        spanBuilder.setEndTime(endTime);
        spanBuilder.setOperationName(operationName);
        spanBuilder.setSkipAnalysis(skipAnalysis);
        spanBuilder.setSpanType(getSpanType());

//        if (isEntry()) {
//            spanBuilder.setSpanType(SpanType.Entry);
//        } else if (isExit()) {
//            spanBuilder.setSpanType(SpanType.Exit);
//        } else {
//            spanBuilder.setSpanType(SpanType.Local);
//        }
        if (this.layer != null) {
            spanBuilder.setSpanLayerValue(this.layer.getCode());
        }
        if (componentId != DictionaryUtil.nullValue()) {
            spanBuilder.setComponentId(componentId);
        }
        spanBuilder.setIsError(errorOccurred);
        if (this.tags != null) {
            for (TagValuePair tag : this.tags) {
                spanBuilder.addTags(tag.transform());
            }
        }
        if (this.logs != null) {
            for (LogDataEntity log : this.logs) {
                spanBuilder.addLogs(log.transform());
            }
        }
        if (this.refs != null) {
            for (TraceSegmentRef ref : this.refs) {
                spanBuilder.addRefs(ref.transform());
            }
        }

        return spanBuilder;
    }

    @Override
    public void ref(TraceSegmentRef ref) {
        if (refs == null) {
            refs = new LinkedList<>();
        }
        /*
         * Provide the OOM protection if the entry span hosts too many references.
         */
        if (refs.size() == Config.Agent.TRACE_SEGMENT_REF_LIMIT_PER_SPAN) {
            return;
        }
        if (!refs.contains(ref)) {
            refs.add(ref);
        }
    }

    @Override
    public AbstractSpan prepareForAsync() {
        if (isInAsyncMode) {
            throw new RuntimeException("Prepare for async repeatedly. Span is already in async mode.");
        }
        ContextManager.awaitFinishAsync(this);
        isInAsyncMode = true;
        return this;
    }

    @Override
    public AbstractSpan asyncFinish() {
        if (!isInAsyncMode) {
            throw new RuntimeException("Span is not in async mode, please use '#prepareForAsync' to active.");
        }
        if (isAsyncStopped) {
            throw new RuntimeException("Can not do async finish for the span repeatedly.");
        }
        this.endTime = System.currentTimeMillis();
        owner.asyncStop(this);
        isAsyncStopped = true;
        return this;
    }

    @Override
    public boolean isProfiling() {
        return this.owner.profileStatus().isProfiling();
    }

    @Override
    public void skipAnalysis() {
        this.skipAnalysis = true;
    }
}
