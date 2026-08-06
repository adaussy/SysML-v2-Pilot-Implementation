/**
 * SysML 2 Pilot Implementation
 * Copyright (C) 2026 Obeo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Eclipse Public License, version 2, as published by
 * the Eclipse Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Eclipse Public License for more details.
 *
 * You should have received a copy of the Eclipse Public License
 * along with this program. If not, see <https://www.eclipse.org/legal/epl-2.0/>.
 *
 * @license EPL-2.0 <http://spdx.org/licenses/EPL-2.0>
 */
package org.omg.sysml.logic;

import org.eclipse.emf.common.util.EList;
import org.omg.sysml.lang.sysml.Connector;
import org.omg.sysml.lang.sysml.ConstructorExpression;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.Expression;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureChainExpression;
import org.omg.sysml.lang.sysml.FeatureDirectionKind;
import org.omg.sysml.lang.sysml.FeatureReferenceExpression;
import org.omg.sysml.lang.sysml.Flow;
import org.omg.sysml.lang.sysml.FlowEnd;
import org.omg.sysml.lang.sysml.ForLoopActionUsage;
import org.omg.sysml.lang.sysml.IndexExpression;
import org.omg.sysml.lang.sysml.InvocationExpression;
import org.omg.sysml.lang.sysml.ReferenceUsage;
import org.omg.sysml.lang.sysml.SelectExpression;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.util.ElementUtil;
import org.omg.sysml.util.ExpressionUtil;
import org.omg.sysml.util.FeatureUtil;
import org.omg.sysml.util.ImplicitGeneralizationMap;
import org.omg.sysml.util.SysMLLibraryUtil;
import org.omg.sysml.util.TypeUtil;

/**
 * Computes implicit specializations that belong to a type but are determined by
 * its owning semantic context.
 *
 * <p>These rules used to mutate another {@code TypeAdapter} while transforming
 * an expression, connector or action. Computing them from the requested type
 * makes transient, uncached reads independent from transformation order.
 */
final class ContextualImplicitSpecializationRules {

	void apply(Type type, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (type instanceof Feature feature) {
			applyExpressionResultRules(feature, result, context);
			applyFeatureChainTargetRules(feature, result);
			applyConnectorEndRules(feature, result);
			applyFlowFeatureRules(feature, result);
			applyLoopVariableRules(feature, result);
		}
	}

	private void applyExpressionResultRules(Feature feature, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		Type owningType = feature.getOwningType();
		if (!(owningType instanceof Expression expression) || expression.getResult() != feature) {
			return;
		}

		if (expression instanceof FeatureReferenceExpression referenceExpression) {
			Element referent = ExpressionUtil.getReferentFor(referenceExpression);
			if (referent instanceof Feature referentFeature) {
				result.add(SysMLPackage.Literals.SUBSETTING, referentFeature);
			}
		} else if (expression instanceof FeatureChainExpression chainExpression) {
			addFeatureChainResultRule(chainExpression, result);
		} else if (expression instanceof IndexExpression indexExpression) {
			addIndexResultRule(indexExpression, result, context);
		} else if (expression instanceof SelectExpression selectExpression) {
			addSelectResultRule(selectExpression, result);
		} else if (expression instanceof ConstructorExpression constructorExpression) {
			addInstantiationResultRule(constructorExpression.getInstantiatedType(), result);
		} else if (expression instanceof InvocationExpression invocationExpression
				&& !isFunctionType(invocationExpression.getInstantiatedType())) {
			addInstantiationResultRule(invocationExpression.getInstantiatedType(), result);
		}
	}

	private void addFeatureChainResultRule(FeatureChainExpression expression,
			ImplicitSpecializationResult result) {
		Feature sourceTarget = expression.sourceTargetFeature();
		Feature sourceParameter = expression.getOwnedFeature().stream()
				.filter(parameter -> parameter.getDirection() == FeatureDirectionKind.IN)
				.findFirst()
				.orElse(null);
		if (sourceParameter != null && sourceTarget != null) {
			result.add(SysMLPackage.Literals.SUBSETTING, FeatureUtil.chainFeatures(sourceParameter, sourceTarget));
		}
	}

	private void addIndexResultRule(IndexExpression expression,
			ImplicitSpecializationResult result, ImplicitSpecializationEvaluationContext context) {
		if (!expression.getArgument().isEmpty()) {
			Expression sequenceExpression = expression.getArgument().get(0);
			ElementUtil.transform(sequenceExpression);
			Feature sequenceResult = sequenceExpression.getResult();
			Type collectionType = SysMLLibraryUtil.getLibraryType(expression, "Collections::Collection");
			boolean isCollection = sequenceResult != null && collectionType != null
					&& ImplicitSpecializationRules.specializes(sequenceResult, collectionType, context);
			if (sequenceResult != null && !isCollection) {
				result.add(SysMLPackage.Literals.SUBSETTING, sequenceResult);
			}
		}
	}

	private void addSelectResultRule(SelectExpression expression,
			ImplicitSpecializationResult result) {
		if (!expression.getArgument().isEmpty()) {
			result.add(SysMLPackage.Literals.SUBSETTING, expression.getArgument().get(0).getResult());
		}
	}

	private void addInstantiationResultRule(Type instantiatedType,
			ImplicitSpecializationResult result) {
		if (instantiatedType instanceof Feature) {
			result.add(SysMLPackage.Literals.SUBSETTING, instantiatedType);
		} else {
			result.add(SysMLPackage.Literals.FEATURE_TYPING, instantiatedType);
		}
	}

	private boolean isFunctionType(Type type) {
		return type instanceof org.omg.sysml.lang.sysml.Function
				|| type instanceof Feature feature
						&& feature.getType().stream().anyMatch(org.omg.sysml.lang.sysml.Function.class::isInstance);
	}

	private void applyFeatureChainTargetRules(Feature feature,
			ImplicitSpecializationResult result) {
		Type owningType = feature.getOwningType();
		if (owningType instanceof Feature sourceParameter
				&& sourceParameter.getOwningType() instanceof FeatureChainExpression expression
				&& expression.sourceTargetFeature() == feature) {
			result.add(SysMLPackage.Literals.REDEFINITION,
					libraryType(expression, ImplicitGeneralizationMap.getDefaultSupertypeFor(
							expression.getClass(), "target")));
			result.add(SysMLPackage.Literals.REDEFINITION, expression.getTargetFeature());
			result.suppressDefaults();
		}
	}

	private void applyConnectorEndRules(Feature feature, ImplicitSpecializationResult result) {
		if (feature.getOwningType() instanceof Connector connector
				&& connector.getOwnedEndFeature().contains(feature)) {
			feature.getOwnedFeature().stream()
					.filter(Expression.class::isInstance)
					.map(Expression.class::cast)
					.map(Expression::getResult)
					.filter(general -> general != null)
					.findFirst()
					.ifPresent(general -> result.add(SysMLPackage.Literals.SUBSETTING, general));
		}
	}

	private void applyFlowFeatureRules(Feature feature, ImplicitSpecializationResult result) {
		if (!(feature.getOwningType() instanceof FlowEnd flowEnd)
				|| flowEnd.getOwnedFeature().stream().findFirst().orElse(null) != feature) {
			return;
		}

		Element owner = flowEnd.getOwner();
		if (owner instanceof Feature ownerFeature) {
			int endIndex = ownerFeature.getEndFeature().indexOf(flowEnd);
			if (endIndex == 0 || endIndex == 1) {
				String kind = endIndex == 0 ? "sourceOutput" : "targetInput";
				result.add(SysMLPackage.Literals.REDEFINITION,
						libraryType(flowEnd, ImplicitGeneralizationMap.getDefaultSupertypeFor(
								flowEnd.getClass(), kind)));
				result.suppressDefaults();
			}
		}

		if (flowEnd.getOwningType() instanceof Flow flow) {
			EList<Feature> ends = flow.getConnectorEnd();
			if (ends.size() >= 2 && ends.get(1) == flowEnd && flow.getOwningNamespace() instanceof Feature flowOwner) {
				result.add(SysMLPackage.Literals.REDEFINITION, flowOwner);
			}
		}
	}

	private void applyLoopVariableRules(Feature feature, ImplicitSpecializationResult result) {
		if (feature instanceof ReferenceUsage loopVariable
				&& loopVariable.getOwningType() instanceof ForLoopActionUsage action
				&& action.getLoopVariable() == loopVariable) {
			result.add(SysMLPackage.Literals.REDEFINITION,
					libraryType(action, ImplicitGeneralizationMap.getDefaultSupertypeFor(
							action.getClass(), "loopVariable")));
			ReferenceUsage sequenceParameter = TypeUtil.getOwnedParameterOf(action, 0, ReferenceUsage.class);
			result.add(SysMLPackage.Literals.SUBSETTING, sequenceParameter);
			result.suppressDefaults();
		}
	}

	private Type libraryType(Element context, String... names) {
		return SysMLLibraryUtil.getLibraryType(context, names);
	}
}
