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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.omg.sysml.lang.sysml.ActionUsage;
import org.omg.sysml.lang.sysml.AssignmentActionUsage;
import org.omg.sysml.lang.sysml.ConstructorExpression;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.Expression;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureMembership;
import org.omg.sysml.lang.sysml.FeatureValue;
import org.omg.sysml.lang.sysml.Multiplicity;
import org.omg.sysml.lang.sysml.PayloadFeature;
import org.omg.sysml.lang.sysml.ReferenceUsage;
import org.omg.sysml.lang.sysml.RenderingUsage;
import org.omg.sysml.lang.sysml.RequirementUsage;
import org.omg.sysml.lang.sysml.StateSubactionMembership;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.lang.sysml.TransitionFeatureMembership;
import org.omg.sysml.lang.sysml.TransitionUsage;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.lang.sysml.VisibilityKind;
import org.omg.sysml.lang.sysml.ViewRenderingMembership;
import org.omg.sysml.util.ExpressionUtil;
import org.omg.sysml.util.FeatureUtil;
import org.omg.sysml.util.ImplicitGeneralizationMap;
import org.omg.sysml.util.SysMLLibraryUtil;
import org.omg.sysml.util.TypeUtil;
import org.omg.sysml.util.UsageUtil;

/** Rules for implicit redefinitions of features. */
final class FeatureImplicitRedefinitionRules {

	private static final String EXPRESSION_GUARD_FEATURE =
			"TransitionPerformances::TransitionPerformance::guard";
	private static final String TRANSITION_LINK_FEATURE =
			"TransitionPerformances::TransitionPerformance::transitionLink";

	void apply(Feature feature, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (!shouldCompute(feature)) {
			return;
		}
		addFeatureWriteRedefinition(feature, result);
		if (feature instanceof PayloadFeature) {
			addMappedRedefinition(feature, result, "payload");
			return;
		}
		if (feature instanceof ReferenceUsage reference && addTransitionLinkRedefinition(reference, result)) {
			return;
		}
		addPositionalRedefinitions(feature, result, context);
		if (feature instanceof RenderingUsage rendering
				&& rendering.getOwningFeatureMembership() instanceof ViewRenderingMembership) {
			addMappedRedefinition(rendering, result, "viewRendering");
		}
	}

	private boolean shouldCompute(Feature feature) {
		String actionFeature = actionRedefinedFeature(feature);
		if (actionFeature != null) {
			return true;
		}
		Type owner = feature.getOwningType();
		return (!(owner instanceof org.omg.sysml.lang.sysml.InvocationExpression
				|| ExpressionUtil.isConstructorResult(owner)) || feature.getOwnedRedefinition().isEmpty());
	}

	private void addFeatureWriteRedefinition(Feature feature, ImplicitSpecializationResult result) {
		if (isStartingAtFeature(feature)) {
			addMappedRedefinition(feature, result, "startingAt");
		} else if (isAccessedFeature(feature)) {
			addMappedRedefinition(feature, result, "accessedFeature");
			AssignmentActionUsage assignment = (AssignmentActionUsage)feature.getOwner().getOwner().getOwner();
			result.add(SysMLPackage.Literals.REDEFINITION, assignment.getReferent());
		}
	}

	private boolean addTransitionLinkRedefinition(ReferenceUsage feature,
			ImplicitSpecializationResult result) {
		Type owner = feature.getOwningType();
		if (owner instanceof TransitionUsage transition
				&& feature == UsageUtil.getTransitionLinkFeatureOf(transition)) {
			result.add(SysMLPackage.Literals.REDEFINITION,
					SysMLLibraryUtil.getLibraryType(feature, TRANSITION_LINK_FEATURE));
			return true;
		}
		return false;
	}

	private void addPositionalRedefinitions(Feature feature, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		Type owner = feature.getOwningType();
		if (owner == null) {
			return;
		}
		List<? extends Feature> ownedRelevant = relevantFeatures(feature, owner);
		int index = ownedRelevant.indexOf(feature);
		if (index < 0) {
			return;
		}
		for (Type general : generalTypes(owner, feature, context)) {
			List<? extends Feature> candidates = relevantFeatures(feature, general);
			if (index < candidates.size() && candidates.get(index) != null
					&& candidates.get(index) != feature) {
				result.add(SysMLPackage.Literals.REDEFINITION, candidates.get(index));
			}
		}
	}

	private List<Type> generalTypes(Type owner, Feature feature,
			ImplicitSpecializationEvaluationContext context) {
		if (feature instanceof Expression expression && ExpressionUtil.isTransitionGuard(expression)) {
			Type general = SysMLLibraryUtil.getLibraryType(owner,
					ImplicitGeneralizationMap.getDefaultSupertypeFor(owner.getClass(), "base"));
			return general == null ? List.of() : List.of(general);
		}
		Set<Type> result = new LinkedHashSet<>();
		collectGeneralTypes(owner, context, result);
		return List.copyOf(result);
	}

	private void collectGeneralTypes(Type type, ImplicitSpecializationEvaluationContext context,
			Set<Type> result) {
		for (Type general : ImplicitSpecializationRules.directGeneralTypes(type, context)) {
			if (result.add(general)) {
				collectGeneralTypes(general, context, result);
			}
		}
	}

	private List<? extends Feature> relevantFeatures(Feature feature, Type type) {
		if (type == null) {
			return List.of();
		}
		String actionFeature = actionRedefinedFeature(feature);
		if (actionFeature != null) {
			return type == feature.getOwningType() ? List.of(feature)
					: libraryFeature(feature, actionFeature);
		}
		if (feature instanceof RequirementUsage requirement && UsageUtil.isObjective(requirement)) {
			return Collections.singletonList(type == feature.getOwningType()
					? UsageUtil.getOwnedObjectiveRequirementOf(type)
					: UsageUtil.getObjectiveRequirementOf(type));
		}
		if (feature instanceof Expression expression) {
			if (ExpressionUtil.isTransitionGuard(expression)) {
				return type == feature.getOwningType() ? List.of(feature)
						: libraryFeature(feature, EXPRESSION_GUARD_FEATURE);
			}
			if (expression.getOwningType() instanceof FeatureValue) {
				return List.of();
			}
		}
		if (feature instanceof Multiplicity) {
			return List.of();
		}
		if (feature.isEnd()) {
			return feature.getOwningType() == type ? type.getOwnedEndFeature() : type.getEndFeature();
		}
		if (ExpressionUtil.isConstructorResult(feature.getOwningType())) {
			return constructorFeatures(feature, type);
		}
		if (FeatureUtil.isParameter(feature)) {
			return parameterFeatures(feature, type);
		}
		return List.of();
	}

	private List<Feature> libraryFeature(Feature context, String qualifiedName) {
		Type libraryType = SysMLLibraryUtil.getLibraryType(context, qualifiedName);
		return libraryType instanceof Feature feature ? List.of(feature) : List.of();
	}

	private List<? extends Feature> constructorFeatures(Feature feature, Type type) {
		Type owner = feature.getOwningType();
		if (type == owner) {
			return type.getOwnedFeature();
		}
		if (owner.getOwningNamespace() instanceof ConstructorExpression constructor
				&& type == constructor.getInstantiatedType()) {
			return type.getFeature().stream()
					.filter(candidate -> candidate.getOwningFeatureMembership() != null
							&& candidate.getOwningFeatureMembership().getVisibility() == VisibilityKind.PUBLIC)
					.toList();
		}
		return List.of();
	}

	private List<Feature> parameterFeatures(Feature feature, Type type) {
		if (FeatureUtil.isResultParameter(feature)) {
			Feature result = TypeUtil.getResultParameterOf(type);
			return result == null ? List.of() : List.of(result);
		}
		List<Feature> parameters = type == feature.getOwningType()
				? TypeUtil.getOwnedParametersOf(type) : TypeUtil.getAllParametersOf(type);
		return parameters.stream().filter(parameter -> !FeatureUtil.isIgnoredParameter(parameter)).toList();
	}

	private String actionRedefinedFeature(Feature feature) {
		FeatureMembership membership = feature.getOwningFeatureMembership();
		String kind = membership instanceof StateSubactionMembership stateMembership
				? stateMembership.getKind().toString()
				: membership instanceof TransitionFeatureMembership transitionMembership
						? transitionMembership.getKind().toString() : null;
		return kind == null ? null
				: ImplicitGeneralizationMap.getDefaultSupertypeFor(feature.getClass(), kind);
	}

	private boolean isStartingAtFeature(Feature feature) {
		Type owner = feature.getOwningType();
		return owner instanceof Feature ownerFeature
				&& ownerFeature.getOwningType() instanceof AssignmentActionUsage assignment
				&& assignment.getParameter().indexOf(owner) == 0;
	}

	private boolean isAccessedFeature(Feature feature) {
		return feature.getOwningType() instanceof Feature owner && isStartingAtFeature(owner)
				&& owner.getOwnedFeature().indexOf(feature) == 0;
	}

	private void addMappedRedefinition(Feature feature, ImplicitSpecializationResult result,
			String key) {
		result.add(SysMLPackage.Literals.REDEFINITION,
				SysMLLibraryUtil.getLibraryType(feature,
						ImplicitGeneralizationMap.getDefaultSupertypeFor(feature.getClass(), key)));
	}
}
