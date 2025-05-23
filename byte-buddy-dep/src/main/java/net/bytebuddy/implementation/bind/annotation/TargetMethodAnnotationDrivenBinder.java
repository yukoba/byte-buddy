/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.implementation.bind.annotation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.constant.DefaultValue;
import net.bytebuddy.utility.ConstantValue;
import net.bytebuddy.utility.JavaConstant;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * This {@link net.bytebuddy.implementation.bind.MethodDelegationBinder} binds
 * method by analyzing annotations found on the <i>target</i> method that is subject to a method binding.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TargetMethodAnnotationDrivenBinder implements MethodDelegationBinder {

    /**
     * The processor for performing an actual method delegation.
     */
    private final DelegationProcessor delegationProcessor;

    /**
     * Creates a new target method annotation-driven binder.
     *
     * @param delegationProcessor The delegation processor to use.
     */
    protected TargetMethodAnnotationDrivenBinder(DelegationProcessor delegationProcessor) {
        this.delegationProcessor = delegationProcessor;
    }

    /**
     * Creates a new method delegation binder that binds method based on annotations found on the target method.
     *
     * @param parameterBinders A list of parameter binder delegates. Each such delegate is responsible for creating a
     *                         {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding}
     *                         for a specific annotation.
     * @return An appropriate method delegation binder.
     */
    public static MethodDelegationBinder of(List<? extends ParameterBinder<?>> parameterBinders) {
        return new TargetMethodAnnotationDrivenBinder(DelegationProcessor.of(parameterBinders));
    }

    /**
     * {@inheritDoc}
     */
    public MethodDelegationBinder.Record compile(MethodDescription candidate) {
        if (IgnoreForBinding.Verifier.check(candidate)) {
            return MethodDelegationBinder.Record.Illegal.INSTANCE;
        }
        List<DelegationProcessor.Handler> handlers = new ArrayList<DelegationProcessor.Handler>(candidate.getParameters().size());
        for (ParameterDescription parameterDescription : candidate.getParameters()) {
            handlers.add(delegationProcessor.prepare(parameterDescription));
        }
        return new Record(candidate, handlers, RuntimeType.Verifier.check(candidate));
    }

    /**
     * A compiled record of a target method annotation-driven binder.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class Record implements MethodDelegationBinder.Record {

        /**
         * The candidate method.
         */
        private final MethodDescription candidate;

        /**
         * A list of handlers for each parameter.
         */
        private final List<DelegationProcessor.Handler> handlers;

        /**
         * The typing to apply.
         */
        private final Assigner.Typing typing;

        /**
         * Creates a default compiled method delegation binder.
         *
         * @param candidate The candidate method.
         * @param handlers  A list of handlers for each parameter.
         * @param typing    The typing to apply.
         */
        protected Record(MethodDescription candidate, List<DelegationProcessor.Handler> handlers, Assigner.Typing typing) {
            this.candidate = candidate;
            this.handlers = handlers;
            this.typing = typing;
        }

        /**
         * {@inheritDoc}
         */
        public MethodBinding bind(Implementation.Target implementationTarget,
                                  MethodDescription source,
                                  MethodDelegationBinder.TerminationHandler terminationHandler,
                                  MethodInvoker methodInvoker,
                                  Assigner assigner) {
            if (!candidate.isAccessibleTo(implementationTarget.getInstrumentedType())) {
                return MethodBinding.Illegal.INSTANCE;
            }
            StackManipulation methodTermination = terminationHandler.resolve(assigner, typing, source, candidate);
            if (!methodTermination.isValid()) {
                return MethodBinding.Illegal.INSTANCE;
            }
            MethodBinding.Builder methodDelegationBindingBuilder = new MethodBinding.Builder(methodInvoker, candidate);
            for (DelegationProcessor.Handler handler : handlers) {
                ParameterBinding<?> parameterBinding = handler.bind(source, implementationTarget, assigner);
                if (!parameterBinding.isValid() || !methodDelegationBindingBuilder.append(parameterBinding)) {
                    return MethodBinding.Illegal.INSTANCE;
                }
            }
            return methodDelegationBindingBuilder.build(methodTermination);
        }

        @Override
        public String toString() {
            return candidate.toString();
        }
    }

    /**
     * A parameter binder is used as a delegate for binding a parameter according to a particular annotation type found
     * on this parameter.
     *
     * @param <T> The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "Safe initialization is implied.")
    public interface ParameterBinder<T extends Annotation> {

        /**
         * The default parameter binders to be used.
         */
        List<ParameterBinder<?>> DEFAULTS = Collections.unmodifiableList(Arrays.<TargetMethodAnnotationDrivenBinder.ParameterBinder<?>>asList(
                Argument.Binder.INSTANCE,
                AllArguments.Binder.INSTANCE,
                Origin.Binder.INSTANCE,
                This.Binder.INSTANCE,
                Super.Binder.INSTANCE,
                Default.Binder.INSTANCE,
                SuperCall.Binder.INSTANCE,
                SuperCallHandle.Binder.INSTANCE,
                DefaultCall.Binder.INSTANCE,
                DefaultCallHandle.Binder.INSTANCE,
                SuperMethod.Binder.INSTANCE,
                SuperMethodHandle.Binder.INSTANCE,
                Handle.Binder.INSTANCE,
                DynamicConstant.Binder.INSTANCE,
                DefaultMethod.Binder.INSTANCE,
                DefaultMethodHandle.Binder.INSTANCE,
                FieldValue.Binder.INSTANCE,
                FieldGetterHandle.Binder.INSTANCE,
                FieldSetterHandle.Binder.INSTANCE,
                StubValue.Binder.INSTANCE,
                Empty.Binder.INSTANCE));

        /**
         * The annotation type that is handled by this parameter binder.
         *
         * @return The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
         */
        Class<T> getHandledType();

        /**
         * Creates a parameter binding for the given target parameter.
         *
         * @param annotation           The annotation that was cause for the delegation to this argument binder.
         * @param source               The intercepted source method.
         * @param target               Tge target parameter that is subject to be bound to
         *                             intercepting the {@code source} method.
         * @param implementationTarget The target of the current implementation that is subject to this binding.
         * @param assigner             An assigner that can be used for applying the binding.
         * @param typing               The typing to apply.
         * @return A parameter binding for the requested target method parameter.
         */
        ParameterBinding<?> bind(AnnotationDescription.Loadable<T> annotation,
                                 MethodDescription source,
                                 ParameterDescription target,
                                 Implementation.Target implementationTarget,
                                 Assigner assigner,
                                 Assigner.Typing typing);

        /**
         * <p>
         * Implements a parameter binder that binds a fixed value to a parameter with a given annotation.
         * </p>
         * <p>
         * This binder is only capable to store values that can either be expressed as Java byte code or as a constant pool value. This
         * includes primitive types, {@link String} values, {@link Class} values which can also be expressed as {@link TypeDescription}
         * instances or method handles and method types for classes of a version at least of Java 7. The latter instances can also be
         * expressed as unloaded {@link JavaConstant} representations.
         * </p>
         * <p>
         * <b>Important</b>: When supplying a method handle or a method type, all types that are implied must be visible to the instrumented
         * type or an {@link IllegalAccessException} will be thrown at runtime.
         * </p>
         *
         * @param <S> The bound annotation's type.
         */
        abstract class ForFixedValue<S extends Annotation> implements ParameterBinder<S> {

            /**
             * {@inheritDoc}
             */
            public ParameterBinding<?> bind(AnnotationDescription.Loadable<S> annotation,
                                            MethodDescription source,
                                            ParameterDescription target,
                                            Implementation.Target implementationTarget,
                                            Assigner assigner,
                                            Assigner.Typing typing) {
                Object value = bind(annotation, source, target);
                if (value == null) {
                    return new ParameterBinding.Anonymous(DefaultValue.of(target.getType()));
                } else {
                    ConstantValue constant = ConstantValue.Simple.wrap(value);
                    return new ParameterBinding.Anonymous(new StackManipulation.Compound(
                            constant.toStackManipulation(),
                            assigner.assign(constant.getTypeDescription().asGenericType(), target.getType(), typing)));
                }
            }

            /**
             * Resolves a value for the given annotation on a parameter that is processed by a {@link net.bytebuddy.implementation.MethodDelegation}.
             *
             * @param annotation The annotation that triggered this binding.
             * @param source     The method for which a delegation is currently bound.
             * @param target     The parameter for which a value is bound.
             * @return The constant pool value that is bound to this parameter or {@code null} for binding this value.
             */
            @MaybeNull
            protected abstract Object bind(AnnotationDescription.Loadable<S> annotation, MethodDescription source, ParameterDescription target);

            /**
             * <p>
             * A parameter binder that binds a fixed value to a parameter annotation when using a {@link net.bytebuddy.implementation.MethodDelegation}.
             * </p>
             * <p>
             * This binder is only capable to store
             * values that can either be expressed as Java byte code or as a constant pool value. This includes primitive types, {@link String} values,
             * {@link Class} values which can also be expressed as {@link TypeDescription} instances or method handles and method types for classes of
             * a version at least of Java 7. The latter instances can also be expressed as unloaded {@link JavaConstant} representations.
             * </p>
             *
             * @param <U> The bound annotation's type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            public static class OfConstant<U extends Annotation> extends ForFixedValue<U> {

                /**
                 * The type of the annotation that is bound by this binder.
                 */
                private final Class<U> type;

                /**
                 * The value that is assigned to any annotated parameter.
                 */
                @MaybeNull
                @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                private final Object value;

                /**
                 * Creates a binder for binding a fixed value to a parameter annotated with the given annotation.
                 *
                 * @param type  The type of the annotation that is bound by this binder.
                 * @param value The value that is assigned to any annotated parameter.
                 */
                protected OfConstant(Class<U> type, @MaybeNull Object value) {
                    this.type = type;
                    this.value = value;
                }

                /**
                 * Creates a binder for binding a fixed value to a given annotation.
                 *
                 * @param type  The type of the annotation that is bound by this binder.
                 * @param value The value that is assigned to any annotated parameter.
                 * @param <V>   The bound annotation's type.
                 * @return A parameter binder that binds the given annotation to the supplied value.
                 */
                public static <V extends Annotation> ParameterBinder<V> of(Class<V> type, @MaybeNull Object value) {
                    return new OfConstant<V>(type, value);
                }

                /**
                 * {@inheritDoc}
                 */
                public Class<U> getHandledType() {
                    return type;
                }

                @MaybeNull
                @Override
                protected Object bind(AnnotationDescription.Loadable<U> annotation, MethodDescription source, ParameterDescription target) {
                    return value;
                }
            }
        }

        /**
         * A parameter binder that binds a field's value.
         *
         * @param <S> The {@link java.lang.annotation.Annotation#annotationType()} handled by this parameter binder.
         */
        abstract class ForFieldBinding<S extends Annotation> implements ParameterBinder<S> {

            /**
             * Indicates that a name should be extracted from an accessor method.
             */
            protected static final String BEAN_PROPERTY = "";

            /**
             * {@inheritDoc}
             */
            public ParameterBinding<?> bind(AnnotationDescription.Loadable<S> annotation,
                                            MethodDescription source,
                                            ParameterDescription target,
                                            Implementation.Target implementationTarget,
                                            Assigner assigner,
                                            Assigner.Typing typing) {
                if (!declaringType(annotation).represents(void.class)) {
                    if (declaringType(annotation).isPrimitive() || declaringType(annotation).isArray()) {
                        throw new IllegalStateException("A primitive type or array type cannot declare a field: " + source);
                    } else if (!implementationTarget.getInstrumentedType().isAssignableTo(declaringType(annotation))) {
                        return MethodDelegationBinder.ParameterBinding.Illegal.INSTANCE;
                    }
                }
                FieldLocator fieldLocator = declaringType(annotation).represents(void.class)
                        ? new FieldLocator.ForClassHierarchy(implementationTarget.getInstrumentedType())
                        : new FieldLocator.ForExactType(declaringType(annotation), implementationTarget.getInstrumentedType());
                FieldLocator.Resolution resolution = fieldName(annotation).equals(BEAN_PROPERTY)
                        ? FieldLocator.Resolution.Simple.ofBeanAccessor(fieldLocator, source)
                        : fieldLocator.locate(fieldName(annotation));
                return resolution.isResolved() && !(source.isStatic() && !resolution.getField().isStatic())
                        ? bind(resolution.getField(), annotation, source, target, implementationTarget, assigner)
                        : ParameterBinding.Illegal.INSTANCE;
            }

            /**
             * Extracts the field name from an annotation.
             *
             * @param annotation The annotation from which to extract the field name.
             * @return The field name defined by the handled annotation.
             */
            protected abstract String fieldName(AnnotationDescription.Loadable<S> annotation);

            /**
             * Extracts the declaring type from an annotation.
             *
             * @param annotation The annotation from which to extract the declaring type.
             * @return The declaring type defined by the handled annotation.
             */
            protected abstract TypeDescription declaringType(AnnotationDescription.Loadable<S> annotation);

            /**
             * Creates a parameter binding for the given target parameter.
             *
             * @param fieldDescription     The field for which this binder binds a value.
             * @param annotation           The annotation that was cause for the delegation to this argument binder.
             * @param source               The intercepted source method.
             * @param target               Tge target parameter that is subject to be bound to
             *                             intercepting the {@code source} method.
             * @param implementationTarget The target of the current implementation that is subject to this binding.
             * @param assigner             An assigner that can be used for applying the binding.
             * @return A parameter binding for the requested target method parameter.
             */
            protected abstract ParameterBinding<?> bind(FieldDescription fieldDescription,
                                                        AnnotationDescription.Loadable<S> annotation,
                                                        MethodDescription source,
                                                        ParameterDescription target,
                                                        Implementation.Target implementationTarget,
                                                        Assigner assigner);
        }
    }

    /**
     * A delegation processor is a helper class for a
     * {@link net.bytebuddy.implementation.bind.annotation.TargetMethodAnnotationDrivenBinder}
     * for performing its actual logic. By outsourcing this logic to this helper class, a cleaner implementation
     * can be provided.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class DelegationProcessor {

        /**
         * A map of registered annotation types to the binder that is responsible for binding a parameter
         * that is annotated with the given annotation.
         */
        private final Map<? extends TypeDescription, ? extends ParameterBinder<?>> parameterBinders;

        /**
         * Creates a new delegation processor.
         *
         * @param parameterBinders A mapping of parameter binders by their handling type.
         */
        protected DelegationProcessor(Map<? extends TypeDescription, ? extends ParameterBinder<?>> parameterBinders) {
            this.parameterBinders = parameterBinders;
        }

        /**
         * Creates a new delegation processor.
         *
         * @param parameterBinders A list of parameter binder delegates. Each such delegate is responsible for creating
         *                         a {@link net.bytebuddy.implementation.bind.MethodDelegationBinder.ParameterBinding}
         *                         for a specific annotation.
         * @return A corresponding delegation processor.
         */
        protected static DelegationProcessor of(List<? extends ParameterBinder<?>> parameterBinders) {
            Map<TypeDescription, ParameterBinder<?>> parameterBinderMap = new HashMap<TypeDescription, ParameterBinder<?>>();
            for (ParameterBinder<?> parameterBinder : parameterBinders) {
                if (parameterBinderMap.put(TypeDescription.ForLoadedType.of(parameterBinder.getHandledType()), parameterBinder) != null) {
                    throw new IllegalArgumentException("Attempt to bind two handlers to " + parameterBinder.getHandledType());
                }
            }
            return new DelegationProcessor(parameterBinderMap);
        }

        /**
         * Locates a handler which is responsible for processing the given parameter. If no explicit handler can
         * be located, a fallback handler is provided.
         *
         * @param target The target parameter being handled.
         * @return A handler for processing the parameter with the given annotations.
         */
        protected Handler prepare(ParameterDescription target) {
            Assigner.Typing typing = RuntimeType.Verifier.check(target);
            Handler handler = new Handler.Unbound(target, typing);
            for (AnnotationDescription annotation : target.getDeclaredAnnotations()) {
                ParameterBinder<?> parameterBinder = parameterBinders.get(annotation.getAnnotationType());
                if (parameterBinder != null && handler.isBound()) {
                    throw new IllegalStateException("Ambiguous binding for parameter annotated with two handled annotation types");
                } else if (parameterBinder != null /* && !handler.isBound() */) {
                    handler = Handler.Bound.of(target, parameterBinder, annotation, typing);
                }
            }
            return handler;
        }

        /**
         * A handler is responsible for processing a parameter's binding.
         */
        protected interface Handler {

            /**
             * Indicates if this handler was explicitly bound.
             *
             * @return {@code true} if this handler was explicitly bound.
             */
            boolean isBound();

            /**
             * Handles a parameter binding.
             *
             * @param source               The intercepted source method.
             * @param implementationTarget The target of the current implementation.
             * @param assigner             The assigner to use.
             * @return A parameter binding that reflects the given arguments.
             */
            ParameterBinding<?> bind(MethodDescription source, Implementation.Target implementationTarget, Assigner assigner);

            /**
             * An unbound handler is a fallback for returning an illegal binding for parameters for which no parameter
             * binder could be located.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Unbound implements Handler {

                /**
                 * The target parameter being handled.
                 */
                private final ParameterDescription target;

                /**
                 * The typing to apply.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new unbound handler.
                 *
                 * @param target The target parameter being handled.
                 * @param typing The typing to apply.
                 */
                protected Unbound(ParameterDescription target, Assigner.Typing typing) {
                    this.target = target;
                    this.typing = typing;
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isBound() {
                    return false;
                }

                /**
                 * {@inheritDoc}
                 */
                public ParameterBinding<?> bind(MethodDescription source, Implementation.Target implementationTarget, Assigner assigner) {
                    return Argument.Binder.INSTANCE.bind(AnnotationDescription.ForLoadedAnnotation.<Argument>of(new DefaultArgument(target.getIndex())),
                            source,
                            target,
                            implementationTarget,
                            assigner,
                            typing);
                }

                /**
                 * A default implementation of an {@link net.bytebuddy.implementation.bind.annotation.Argument} annotation.
                 */
                protected static class DefaultArgument implements Argument {

                    /**
                     * The name of the value annotation parameter.
                     */
                    private static final String VALUE = "value";

                    /**
                     * The name of the value binding mechanic parameter.
                     */
                    private static final String BINDING_MECHANIC = "bindingMechanic";

                    /**
                     * The index of the source method parameter to be bound.
                     */
                    private final int parameterIndex;

                    /**
                     * Creates a new instance of an argument annotation.
                     *
                     * @param parameterIndex The index of the source method parameter to be bound.
                     */
                    protected DefaultArgument(int parameterIndex) {
                        this.parameterIndex = parameterIndex;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public int value() {
                        return parameterIndex;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public BindingMechanic bindingMechanic() {
                        return BindingMechanic.UNIQUE;
                    }

                    /**
                     * {@inheritDoc}
                     */
                    public Class<Argument> annotationType() {
                        return Argument.class;
                    }

                    @Override
                    public int hashCode() {
                        return ((127 * BINDING_MECHANIC.hashCode()) ^ BindingMechanic.UNIQUE.hashCode()) + ((127 * VALUE.hashCode()) ^ parameterIndex);
                    }

                    @Override
                    public boolean equals(@MaybeNull Object other) {
                        return this == other || other instanceof Argument && parameterIndex == ((Argument) other).value();
                    }

                    @Override
                    public String toString() {
                        return "@" + Argument.class.getName()
                                + "(bindingMechanic=" + BindingMechanic.UNIQUE
                                + ", value=" + parameterIndex + ")";
                    }
                }
            }

            /**
             * A bound handler represents an unambiguous parameter binder that was located for a given array of
             * annotations.
             *
             * @param <T> The annotation type of a given handler.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class Bound<T extends Annotation> implements Handler {

                /**
                 * The target parameter being handled.
                 */
                private final ParameterDescription target;

                /**
                 * The parameter binder that is actually responsible for binding the parameter.
                 */
                private final ParameterBinder<T> parameterBinder;

                /**
                 * The annotation value that lead to the binding of this handler.
                 */
                private final AnnotationDescription.Loadable<T> annotation;

                /**
                 * The typing to apply.
                 */
                private final Assigner.Typing typing;

                /**
                 * Creates a new bound handler.
                 *
                 * @param target          The target parameter being handled.
                 * @param parameterBinder The parameter binder that is actually responsible for binding the parameter.
                 * @param annotation      The annotation value that lead to the binding of this handler.
                 * @param typing          The typing to apply.
                 */
                protected Bound(ParameterDescription target,
                                ParameterBinder<T> parameterBinder,
                                AnnotationDescription.Loadable<T> annotation,
                                Assigner.Typing typing) {
                    this.target = target;
                    this.parameterBinder = parameterBinder;
                    this.annotation = annotation;
                    this.typing = typing;
                }

                /**
                 * Creates a handler for a given annotation.
                 *
                 * @param target          The target parameter being handled.
                 * @param parameterBinder The parameter binder that should process an annotation.
                 * @param annotation      An annotation instance that can be understood by this parameter binder.
                 * @param typing          The typing to apply.
                 * @return A handler for processing the given annotation.
                 */
                @SuppressWarnings("unchecked")
                protected static Handler of(ParameterDescription target,
                                            ParameterBinder<?> parameterBinder,
                                            AnnotationDescription annotation,
                                            Assigner.Typing typing) {
                    return new Bound<Annotation>(target,
                            (ParameterBinder<Annotation>) parameterBinder,
                            (AnnotationDescription.Loadable<Annotation>) annotation.prepare(parameterBinder.getHandledType()),
                            typing);
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean isBound() {
                    return true;
                }

                /**
                 * {@inheritDoc}
                 */
                public ParameterBinding<?> bind(MethodDescription source, Implementation.Target implementationTarget, Assigner assigner) {
                    return parameterBinder.bind(annotation,
                            source,
                            target,
                            implementationTarget,
                            assigner,
                            typing);
                }
            }
        }
    }
}
