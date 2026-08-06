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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.omg.sysml.lang.sysml.AcceptActionUsage;
import org.omg.sysml.lang.sysml.ActionDefinition;
import org.omg.sysml.lang.sysml.ActionUsage;
import org.omg.sysml.lang.sysml.AssignmentActionUsage;
import org.omg.sysml.lang.sysml.Association;
import org.omg.sysml.lang.sysml.AssertConstraintUsage;
import org.omg.sysml.lang.sysml.AnalysisCaseDefinition;
import org.omg.sysml.lang.sysml.AnalysisCaseUsage;
import org.omg.sysml.lang.sysml.Behavior;
import org.omg.sysml.lang.sysml.CaseDefinition;
import org.omg.sysml.lang.sysml.CaseUsage;
import org.omg.sysml.lang.sysml.CalculationDefinition;
import org.omg.sysml.lang.sysml.CalculationUsage;
import org.omg.sysml.lang.sysml.Classifier;
import org.omg.sysml.lang.sysml.ConcernUsage;
import org.omg.sysml.lang.sysml.ConnectionDefinition;
import org.omg.sysml.lang.sysml.ConnectionUsage;
import org.omg.sysml.lang.sysml.Connector;
import org.omg.sysml.lang.sysml.ConstraintUsage;
import org.omg.sysml.lang.sysml.CrossSubsetting;
import org.omg.sysml.lang.sysml.DataType;
import org.omg.sysml.lang.sysml.DecisionNode;
import org.omg.sysml.lang.sysml.Definition;
import org.omg.sysml.lang.sysml.Element;
import org.omg.sysml.lang.sysml.EventOccurrenceUsage;
import org.omg.sysml.lang.sysml.ExhibitStateUsage;
import org.omg.sysml.lang.sysml.Expression;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureMembership;
import org.omg.sysml.lang.sysml.FeatureTyping;
import org.omg.sysml.lang.sysml.FeatureValue;
import org.omg.sysml.lang.sysml.Flow;
import org.omg.sysml.lang.sysml.FlowDefinition;
import org.omg.sysml.lang.sysml.FlowEnd;
import org.omg.sysml.lang.sysml.FlowUsage;
import org.omg.sysml.lang.sysml.IfActionUsage;
import org.omg.sysml.lang.sysml.IncludeUseCaseUsage;
import org.omg.sysml.lang.sysml.Invariant;
import org.omg.sysml.lang.sysml.InvocationExpression;
import org.omg.sysml.lang.sysml.ItemDefinition;
import org.omg.sysml.lang.sysml.ItemUsage;
import org.omg.sysml.lang.sysml.MergeNode;
import org.omg.sysml.lang.sysml.MetadataFeature;
import org.omg.sysml.lang.sysml.MetadataUsage;
import org.omg.sysml.lang.sysml.Multiplicity;
import org.omg.sysml.lang.sysml.OccurrenceDefinition;
import org.omg.sysml.lang.sysml.OccurrenceUsage;
import org.omg.sysml.lang.sysml.OperatorExpression;
import org.omg.sysml.lang.sysml.PartDefinition;
import org.omg.sysml.lang.sysml.PartUsage;
import org.omg.sysml.lang.sysml.PerformActionUsage;
import org.omg.sysml.lang.sysml.PayloadFeature;
import org.omg.sysml.lang.sysml.PortionKind;
import org.omg.sysml.lang.sysml.PortDefinition;
import org.omg.sysml.lang.sysml.PortUsage;
import org.omg.sysml.lang.sysml.ReferenceUsage;
import org.omg.sysml.lang.sysml.RenderingDefinition;
import org.omg.sysml.lang.sysml.RenderingUsage;
import org.omg.sysml.lang.sysml.RequirementDefinition;
import org.omg.sysml.lang.sysml.RequirementUsage;
import org.omg.sysml.lang.sysml.SatisfyRequirementUsage;
import org.omg.sysml.lang.sysml.Specialization;
import org.omg.sysml.lang.sysml.StateDefinition;
import org.omg.sysml.lang.sysml.StateSubactionMembership;
import org.omg.sysml.lang.sysml.StateUsage;
import org.omg.sysml.lang.sysml.Step;
import org.omg.sysml.lang.sysml.Structure;
import org.omg.sysml.lang.sysml.Subsetting;
import org.omg.sysml.lang.sysml.SuccessionAsUsage;
import org.omg.sysml.lang.sysml.SysMLFactory;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.lang.sysml.TransitionFeatureMembership;
import org.omg.sysml.lang.sysml.TransitionUsage;
import org.omg.sysml.lang.sysml.TriggerInvocationExpression;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.lang.sysml.Usage;
import org.omg.sysml.lang.sysml.UseCaseDefinition;
import org.omg.sysml.lang.sysml.UseCaseUsage;
import org.omg.sysml.lang.sysml.VerificationCaseDefinition;
import org.omg.sysml.lang.sysml.VerificationCaseUsage;
import org.omg.sysml.lang.sysml.ViewDefinition;
import org.omg.sysml.lang.sysml.ViewRenderingMembership;
import org.omg.sysml.lang.sysml.ViewUsage;
import org.omg.sysml.lang.sysml.ViewpointUsage;
import org.omg.sysml.logic.api.ImplicitSpecialization;
import org.omg.sysml.util.ElementUtil;
import org.omg.sysml.util.EvaluationUtil;
import org.omg.sysml.util.ExpressionUtil;
import org.omg.sysml.util.FeatureUtil;
import org.omg.sysml.util.ImplicitGeneralizationMap;
import org.omg.sysml.util.SysMLLibraryUtil;
import org.omg.sysml.util.TypeUtil;
import org.omg.sysml.util.UsageUtil;

/** All domain rules that infer implicit specialization relationships. */
final class ImplicitSpecializationRules {

	private final FeatureImplicitRedefinitionRules redefinitionRules = new FeatureImplicitRedefinitionRules();

	void apply(Type type, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (type instanceof Feature feature) {
			addPreDefaultFeatureRules(feature, result, context);
		}
		if (result.areDefaultsAllowed() && !type.isConjugated()) {
			addSemanticMetadataBaseTypes(type, result, context);
		}
		if (type instanceof Feature feature) {
			redefinitionRules.apply(feature, result, context);
		}
		if (result.areDefaultsAllowed() && !type.isConjugated()) {
			addDefault(type, result, defaultKind(type), defaultKey(type, context));
			addAdditionalDefaults(type, result, context);
		}
	}

	private void addPreDefaultFeatureRules(Feature feature, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (feature instanceof FlowEnd flowEnd) {
			addFlowEndSubsetting(flowEnd, result);
			result.suppressDefaults();
			return;
		}
		if (feature instanceof AcceptActionUsage accept && isTriggerAction(accept)) {
			result.suppressDefaults();
			return;
		}
		if (feature instanceof ReferenceUsage reference && addTransitionPayloadSubsetting(reference, result)) {
			result.suppressDefaults();
			return;
		}
		if (feature instanceof Usage usage && UsageUtil.isVariant(usage)) {
			Definition definition = UsageUtil.getOwningVariationDefinitionFor(usage);
			if (definition != null) {
				result.add(SysMLPackage.Literals.FEATURE_TYPING, definition);
			} else {
				result.add(SysMLPackage.Literals.SUBSETTING,
						UsageUtil.getOwningVariationUsageFor(usage));
			}
		}
		addOwnedCrossFeatureSpecializations(feature, result, context);

		if (feature instanceof OperatorExpression operator && operator.getOperator() != null) {
			addLibrary(result, feature, SysMLPackage.Literals.FEATURE_TYPING,
					ExpressionUtil.getOperatorQualifiedNames(operator.getOperator()));
		}
		if (feature instanceof TriggerInvocationExpression trigger && trigger.getKind() != null) {
			addLibrary(result, feature, SysMLPackage.Literals.FEATURE_TYPING,
					mapped(feature, trigger.getKind().toString()));
		}
		if (feature instanceof SatisfyRequirementUsage satisfy) {
			Type owner = satisfy.getOwningType();
			if ((owner instanceof ViewDefinition || owner instanceof ViewUsage)
					&& UsageUtil.getSatisfyingFeatureValueOf(satisfy) == null
					&& satisfy.getSatisfiedRequirement() instanceof ViewpointUsage viewpoint) {
				addLibrary(result, feature, SysMLPackage.Literals.SUBSETTING,
						mapped(viewpoint, "satisfied"));
			}
		}
		if (feature instanceof ConstraintUsage constraint) {
			addRequirementConstraintSpecialization(constraint, result);
		}
	}

	private void addRequirementConstraintSpecialization(ConstraintUsage constraint,
			ImplicitSpecializationResult result) {
		if (constraint instanceof RequirementUsage requirement
				&& UsageUtil.isVerifiedRequirement(requirement)) {
			addLibrary(result, constraint, SysMLPackage.Literals.SUBSETTING,
					mapped(constraint, "verification"));
		} else if (constraint instanceof ConcernUsage concern && UsageUtil.isFramedConcern(concern)) {
			addLibrary(result, constraint, SysMLPackage.Literals.SUBSETTING,
					mapped(constraint, "concern"));
		} else if (UsageUtil.getRequirementConstraintKindOf(constraint) != null) {
			addLibrary(result, constraint, SysMLPackage.Literals.SUBSETTING,
					mapped(constraint, UsageUtil.getRequirementConstraintKindOf(constraint).toString()));
		}
	}

	private void addAdditionalDefaults(Type type, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (type instanceof Feature feature) {
			addFeatureDefaults(feature, result, context);
		}
		if (type instanceof OccurrenceDefinition occurrence && occurrence.isIndividual()) {
			addMapped(result, occurrence, defaultKind(type), "life");
		}
		if (type instanceof Association association) {
			// Its binary/base choice is handled by defaultKey.
		} else if (type instanceof ConnectionDefinition connection) {
			// Its binary/base choice is handled by defaultKey.
		} else if (type instanceof FlowDefinition flow) {
			// Its binary/base choice is handled by defaultKey.
		}
	}

	private void addFeatureDefaults(Feature feature, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		addBoundValueSubsetting(feature, result);
		addParticipantSubsetting(feature, result);
		addCrossingSpecialization(feature, result);

		if (feature instanceof InvocationExpression invocation && invocation.getInstantiatedType() != null) {
			EClass kind = invocation.getInstantiatedType() instanceof Feature
					? SysMLPackage.Literals.SUBSETTING
					: SysMLPackage.Literals.FEATURE_TYPING;
			result.add(kind, invocation.getInstantiatedType());
		}
		if (feature instanceof OccurrenceUsage occurrence) {
			addOccurrenceDefaults(occurrence, result, context);
		}
		if (feature instanceof ActionUsage action) {
			addActionDefaults(action, result, context);
		}
		if (feature instanceof FlowUsage flow) {
			addFlowUsageDefaults(flow, result, context);
		}
		if (feature instanceof Step step && !(feature instanceof Expression)
				&& !(feature instanceof ActionUsage)) {
			addPerformanceDefaults(step, result, false, context);
		}
		if (feature instanceof Expression expression) {
			addPerformanceDefaults(expression, result, true, context);
		}
		if (feature instanceof ConstraintUsage constraint) {
			Type owner = constraint.getOwningType();
			if (constraint.isComposite() && (owner instanceof ItemDefinition || owner instanceof ItemUsage)) {
				addMapped(result, constraint, defaultKind(feature), "checkedConstraint");
			}
			addPerformanceDefaults(constraint, result, true, context);
		}
		if (feature instanceof Flow flow) {
			addPerformanceDefaults(flow, result, true, context);
		}
		if (feature instanceof PortUsage port && isStructureOwnedComposite(port, context)) {
			addMapped(result, port, defaultKind(feature), "subobject");
		}
		if ((feature instanceof ConnectionUsage || feature instanceof ViewUsage
				|| feature instanceof RenderingUsage) && isSubitem((ItemUsage)feature)) {
			addMapped(result, feature, defaultKind(feature), "subpart");
		}
		if (feature instanceof IfActionUsage ifAction && ifAction.getElseAction() != null) {
			addMapped(result, feature, defaultKind(feature), "ifThenElse");
		}
		if (feature instanceof ExhibitStateUsage exhibit && isPerformedAction(exhibit)) {
			addMapped(result, feature, defaultKind(feature), "performedAction");
		}
		if (feature instanceof PerformActionUsage perform && isPerformedAction(perform)) {
			addMapped(result, feature, defaultKind(feature), "performedAction");
		}
		if (feature instanceof IncludeUseCaseUsage include && isPerformedAction(include)) {
			addMapped(result, feature, defaultKind(feature), "performedAction");
		}
		if (feature instanceof ConcernUsage concern && UsageUtil.isSubrequirement(concern)) {
			addMapped(result, feature, defaultKind(feature), "subrequirement");
		}
		if (feature instanceof ReferenceUsage reference) {
			addReferenceUsageDefaults(reference, result);
		}
		if (feature instanceof SuccessionAsUsage succession) {
			addSuccessionDefaults(succession, result);
		}
	}

	private void addOccurrenceDefaults(OccurrenceUsage occurrence,
			ImplicitSpecializationResult result, ImplicitSpecializationEvaluationContext context) {
		if (hasDataType(occurrence, context)) {
			addMapped(result, occurrence, defaultKind(occurrence), "dataValue");
		}
		if (hasStructureType(occurrence, context)) {
			addMapped(result, occurrence, defaultKind(occurrence),
					isSubobject(occurrence, context) ? "subobject" : "object");
		} else if (isSuboccurrence(occurrence, context)) {
			addMapped(result, occurrence, defaultKind(occurrence), "suboccurrence");
		}
		if (occurrence.getPortionKind() == PortionKind.SNAPSHOT) {
			addMapped(result, occurrence, defaultKind(occurrence), "snapshot");
		} else if (occurrence.getPortionKind() == PortionKind.TIMESLICE) {
			addMapped(result, occurrence, defaultKind(occurrence), "timeslice");
		}
	}

	private void addActionDefaults(ActionUsage action, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (action instanceof AcceptActionUsage accept && isTriggerAction(accept)) {
			return;
		}
		if (action instanceof AnalysisCaseUsage analysis && analysis.isComposite()
				&& (analysis.getOwningType() instanceof AnalysisCaseDefinition
						|| analysis.getOwningType() instanceof AnalysisCaseUsage)) {
			addMapped(result, action, defaultKind(action), "subAnalysisCase");
		} else if (action instanceof VerificationCaseUsage verification && verification.isComposite()
				&& (verification.getOwningType() instanceof VerificationCaseDefinition
						|| verification.getOwningType() instanceof VerificationCaseUsage)) {
			addMapped(result, action, defaultKind(action), "subVerificationCase");
		} else if (action instanceof UseCaseUsage useCase && useCase.isComposite()
				&& (useCase.getOwningType() instanceof UseCaseDefinition
						|| useCase.getOwningType() instanceof UseCaseUsage)) {
			addMapped(result, action, defaultKind(action), "subUseCase");
		} else if (action instanceof CaseUsage caseUsage && caseUsage.isComposite()
				&& (caseUsage.getOwningType() instanceof CaseDefinition
						|| caseUsage.getOwningType() instanceof CaseUsage)) {
			addMapped(result, action, defaultKind(action), "subcase");
		} else if (action instanceof CalculationUsage calculation && calculation.isComposite()
				&& (calculation.getOwningType() instanceof CalculationDefinition
						|| calculation.getOwningType() instanceof CalculationUsage)) {
			addMapped(result, action, defaultKind(action), "subcalculation");
		} else if (action instanceof StateUsage state && state.isSubstateUsage(false)) {
			addMapped(result, action, defaultKind(action), "exclusiveState");
		} else if (action instanceof StateUsage state && state.isSubstateUsage(true)) {
			addMapped(result, action, defaultKind(action), "substate");
		} else if (isActionOwnedComposite(action)) {
			addMapped(result, action, defaultKind(action), "subaction");
		} else if (isPartOwnedComposite(action)) {
			addMapped(result, action, defaultKind(action), "ownedAction");
		}
		addPerformanceDefaults(action, result, false, context);
	}

	private void addFlowUsageDefaults(FlowUsage flow, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (flow.isComposite() && flow.getOwningType() instanceof OccurrenceUsage) {
			addMapped(result, flow, defaultKind(flow), "suboccurrence");
		}
		if (flow.getPortionKind() == PortionKind.SNAPSHOT) {
			addMapped(result, flow, defaultKind(flow), "snapshot");
		} else if (flow.getPortionKind() == PortionKind.TIMESLICE) {
			addMapped(result, flow, defaultKind(flow), "timeslice");
		}
		if (isActionOwnedComposite(flow)) {
			addMapped(result, flow, defaultKind(flow), "subaction");
		} else if (isPartOwnedComposite(flow)) {
			addMapped(result, flow, defaultKind(flow), "ownedAction");
		}
		addPerformanceDefaults(flow, result, false, context);
	}

	private void addPerformanceDefaults(Feature feature, ImplicitSpecializationResult result,
			boolean independent, ImplicitSpecializationEvaluationContext context) {
		boolean structure = isStructureOwnedComposite(feature, context);
		boolean behaviorComposite = isBehaviorOwned(feature) && feature.isComposite();
		boolean behavior = isBehaviorOwned(feature);
		if (structure) {
			addMapped(result, feature, defaultKind(feature), "ownedPerformance");
		}
		if ((independent || !structure) && behaviorComposite) {
			addMapped(result, feature, defaultKind(feature), "subperformance");
		}
		if ((independent || !structure && !behaviorComposite) && behavior) {
			addMapped(result, feature, defaultKind(feature), "enclosedPerformance");
		}
	}

	private void addReferenceUsageDefaults(ReferenceUsage reference,
			ImplicitSpecializationResult result) {
		Type owner = reference.getOwningType();
		if (owner instanceof SuccessionAsUsage succession) {
			int index = succession.getOwnedEndFeature().indexOf(reference);
			if (index < 2 && reference.getOwnedReferenceSubsetting() == null
					&& !result.containsKind(SysMLPackage.Literals.REFERENCE_SUBSETTING)) {
				result.add(SysMLPackage.Literals.REFERENCE_SUBSETTING,
						index == 0 ? UsageUtil.getSourceFeature(succession)
								: UsageUtil.getTargetFeature(succession));
			}
		}
	}

	private boolean addTransitionPayloadSubsetting(ReferenceUsage reference,
			ImplicitSpecializationResult result) {
		if (reference.getOwningType() instanceof TransitionUsage transition
				&& reference == UsageUtil.getPayloadParameterOf(transition)) {
			Feature accepter = UsageUtil.getAccepterPayloadParameterOf(transition);
			if (accepter != null) {
				result.add(SysMLPackage.Literals.SUBSETTING,
						FeatureUtil.chainFeatures((Feature)accepter.getOwningType(), accepter));
				reference.setDeclaredName(accepter.getDeclaredName());
				return true;
			}
		}
		return false;
	}

	private void addFlowEndSubsetting(FlowEnd flowEnd, ImplicitSpecializationResult result) {
		if (flowEnd.getOwnedSubsetting().isEmpty() && !flowEnd.getOwnedFeature().isEmpty()) {
			FeatureUtil.getRedefinedFeaturesOf(flowEnd.getOwnedFeature().get(0)).stream()
					.findFirst().map(Feature::getOwningType).filter(Feature.class::isInstance)
					.ifPresent(owner -> result.add(SysMLPackage.Literals.SUBSETTING, owner));
		}
	}

	private void addSuccessionDefaults(SuccessionAsUsage succession,
			ImplicitSpecializationResult result) {
		Feature source = UsageUtil.getSourceOf(succession);
		if (source instanceof DecisionNode) {
			result.add(defaultKind(succession), FeatureUtil.chainFeatures(source,
					(Feature)library(result, succession, mapped(succession, "decision"))));
		}
		Feature target = UsageUtil.getTargetOf(succession);
		if (target instanceof MergeNode) {
			result.add(defaultKind(succession), FeatureUtil.chainFeatures(target,
					(Feature)library(result, succession, mapped(succession, "merge"))));
		}
	}

	private String defaultKey(Type type, ImplicitSpecializationEvaluationContext context) {
		if (type instanceof SatisfyRequirementUsage satisfy) {
			return satisfy.isNegated() ? "negated" : "base";
		}
		if (type instanceof Invariant invariant) {
			return invariant.isNegated() ? "negated" : "base";
		}
		if (type instanceof AssertConstraintUsage assertion) {
			return assertion.isNegated() ? "negated" : "base";
		}
		if (type instanceof ViewpointUsage viewpoint) {
			Type owner = viewpoint.getOwningType();
			return owner instanceof ViewDefinition || owner instanceof ViewUsage ? "satisfied" : "base";
		}
		if (type instanceof RequirementUsage requirement) {
			return UsageUtil.isSubrequirement(requirement) ? "subrequirement" : "base";
		}
		if (type instanceof RenderingUsage rendering) {
			Type owner = rendering.getOwningType();
			return owner instanceof RenderingDefinition || owner instanceof RenderingUsage
					? "subrendering" : "base";
		}
		if (type instanceof ViewUsage view) {
			Type owner = view.getOwningType();
			return owner instanceof ViewDefinition || owner instanceof ViewUsage ? "subview" : "base";
		}
		if (type instanceof PortUsage port) {
			Type owner = port.getOwningType();
			return owner instanceof PartDefinition || owner instanceof PartUsage ? "ownedPort"
					: port.isComposite() && (owner instanceof PortDefinition || owner instanceof PortUsage)
							? "subport" : "base";
		}
		if (type instanceof PartUsage part) {
			Type owner = part.getOwningType();
			if (UsageUtil.isActorParameter(part)
					&& (owner instanceof RequirementDefinition || owner instanceof RequirementUsage)) {
				return "requirementActor";
			}
			if (UsageUtil.isStakeholderParameter(part)
					&& (owner instanceof RequirementDefinition || owner instanceof RequirementUsage)) {
				return "requirementStakeholder";
			}
			if (UsageUtil.isActorParameter(part)
					&& (owner instanceof CaseDefinition || owner instanceof CaseUsage)) {
				return "caseActor";
			}
		}
		if (type instanceof ConnectionUsage connection) {
			return connection.getOwnedEndFeature().size() == 2 ? "binary" : "base";
		}
		if (type instanceof ItemUsage item) {
			return isSubitem(item) ? "subitem" : "base";
		}
		if (type instanceof TransitionUsage transition) {
			Type owner = transition.getOwningType();
			if (transition.isComposite() && (owner instanceof StateDefinition || owner instanceof StateUsage)
					&& transition.getSource() instanceof StateUsage) {
				return "stateTransition";
			}
			if (transition.isComposite() && (owner instanceof ActionDefinition || owner instanceof ActionUsage)
					&& !(transition.getSource() instanceof StateUsage)) {
				return "actionTransition";
			}
		}
		if (type instanceof EventOccurrenceUsage event) {
			Type owner = event.getOwningType();
			return owner instanceof OccurrenceDefinition || owner instanceof OccurrenceUsage
					? "suboccurrence" : "base";
		}
		if (type instanceof FlowUsage flowUsage) {
			return UsageUtil.isMessageConnection(flowUsage) ? "message" : "base";
		}
		if (type instanceof Flow flow) {
			return flow.getOwnedEndFeature().isEmpty() ? "base" : "flow";
		}
		if (type instanceof Connector connector) {
			int ends = connector.getOwnedEndFeature().size();
			return hasStructureType(connector, context) ? ends == 2 ? "binaryObject" : "object"
					: ends == 2 ? "binary" : "base";
		}
		if (type instanceof ConnectionDefinition connection) {
			return connection.getOwnedEndFeature().size() == 2 ? "binary" : "base";
		}
		if (type instanceof FlowDefinition flow) {
			return flow.getOwnedEndFeature().size() == 2 ? "binary" : "base";
		}
		if (type instanceof Association association) {
			return association.getOwnedEndFeature().size() == 2 ? "binary" : "base";
		}
		if (type instanceof Multiplicity multiplicity) {
			return multiplicity.getOwner() instanceof Classifier ? "classifier"
					: multiplicity.getOwner() instanceof Feature ? "feature" : "base";
		}
		if (type instanceof Expression || type instanceof ActionUsage || type instanceof OccurrenceUsage
				|| type instanceof ConstraintUsage || type instanceof MetadataFeature
				|| type instanceof MetadataUsage || type instanceof ConcernUsage) {
			return "base";
		}
		if (type instanceof Step step) {
			if (isStructureOwnedComposite(step, context)) {
				return "ownedPerformance";
			}
			if (isBehaviorOwned(step) && step.isComposite()) {
				return "subperformance";
			}
			if (isBehaviorOwned(step)) {
				return "enclosedPerformance";
			}
			if (step.getOwnedFeature().stream().anyMatch(PayloadFeature.class::isInstance)) {
				return "incomingTransfer";
			}
		}
		if (type instanceof Feature feature) {
			if (hasStructureType(feature, context)) {
				return isSubobject(feature, context) ? "subobject" : "object";
			}
			if (hasClassType(feature, context)) {
				return isSuboccurrence(feature, context) ? "suboccurrence"
						: feature.isPortion() ? "portion" : "occurrence";
			}
			return hasDataType(feature, context) ? "dataValue" : "base";
		}
		return "base";
	}

	private EClass defaultKind(Type type) {
		return type instanceof Feature ? SysMLPackage.Literals.SUBSETTING
				: type instanceof Classifier ? SysMLPackage.Literals.SUBCLASSIFICATION
						: SysMLPackage.Literals.SPECIALIZATION;
	}

	private void addSemanticMetadataBaseTypes(Type type, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		if (type instanceof MetadataFeature || !context.enterMetadataEvaluation(type)) {
			return;
		}
		try {
			for (MetadataFeature metadata : ElementUtil.getAllMetadataFeaturesOf(type)) {
				metadata.getMetaclass();
				Feature baseTypeFeature = (Feature)library(result, metadata, mapped(metadata, "baseType"));
				metadata.getFeature().stream()
						.filter(feature -> specializes(feature, baseTypeFeature, context))
						.map(FeatureUtil::getValueExpressionFor)
						.filter(expression -> expression != null)
						.map(expression -> expression.evaluate(metadata))
						.filter(values -> values != null && !values.isEmpty())
						.map(values -> values.get(0))
						.map(EvaluationUtil::getMetaclassReferenceOf)
						.filter(Type.class::isInstance)
						.map(Type.class::cast)
						.forEach(base -> addSemanticMetadataBaseType(type, base, result));
			}
		} finally {
			context.leaveMetadataEvaluation(type);
		}
	}

	private void addSemanticMetadataBaseType(Type type, Type base,
			ImplicitSpecializationResult result) {
		if (type instanceof Feature) {
			if (base instanceof Feature) {
				result.add(defaultKind(type), base);
			}
		} else if (type instanceof Classifier) {
			if (base instanceof Feature feature) {
				feature.getType().stream()
						.filter(Classifier.class::isInstance)
						.forEach(general -> result.add(defaultKind(type), general));
			} else if (base instanceof Classifier) {
				result.add(defaultKind(type), base);
			}
		} else {
			result.add(defaultKind(type), base);
		}
	}

	private void addOwnedCrossFeatureSpecializations(Feature feature,
			ImplicitSpecializationResult result, ImplicitSpecializationEvaluationContext context) {
		if (!FeatureUtil.isOwnedCrossFeature(feature) || !(feature.getOwningNamespace() instanceof Feature owner)) {
			return;
		}
		owner.getType().forEach(type -> result.add(SysMLPackage.Literals.FEATURE_TYPING, type));
		for (Feature redefined : FeatureUtil.getRedefinedFeaturesWithComputedOf(owner)) {
			if (redefined.isEnd()) {
				Feature cross = getCrossFeatureOf(redefined, context);
				if (cross != null) {
					result.add(SysMLPackage.Literals.SUBSETTING, cross);
				}
			}
		}
	}

	private Feature getCrossFeatureOf(Feature feature, ImplicitSpecializationEvaluationContext context) {
		if (feature.getCrossFeature() != null) {
			return feature.getCrossFeature();
		}
		return context.evaluate(feature).stream()
				.filter(specialization -> specialization.specializationKind() == SysMLPackage.Literals.CROSS_SUBSETTING)
				.map(ImplicitSpecialization::generalType)
				.filter(Feature.class::isInstance)
				.map(Feature.class::cast)
				.map(FeatureUtil::getBasicFeatureOf)
				.findFirst().orElse(null);
	}

	private void addBoundValueSubsetting(Feature feature, ImplicitSpecializationResult result) {
		FeatureValue valuation = FeatureUtil.getValuationFor(feature);
		if (valuation != null && !valuation.isDefault() && valuation.getValue() != null
				&& feature.getOwnedSpecialization().isEmpty() && feature.getDirection() == null) {
			Expression value = valuation.getValue();
			ElementUtil.transform(value);
			if (value.getResult() != null) {
				result.add(SysMLPackage.Literals.SUBSETTING,
						FeatureUtil.chainFeatures(value, value.getResult()));
			}
		}
	}

	private void addParticipantSubsetting(Feature feature, ImplicitSpecializationResult result) {
		Type endOwner = feature.getEndOwningType();
		if ((endOwner instanceof Association || endOwner instanceof Connector)
				&& !result.containsKind(SysMLPackage.Literals.REDEFINITION)) {
			addMapped(result, feature, defaultKind(feature), "participant");
		}
	}

	private void addCrossingSpecialization(Feature feature, ImplicitSpecializationResult result) {
		Feature cross = FeatureUtil.getOwnedCrossFeatureOf(feature);
		if (cross == null || feature.getOwnedCrossSubsetting() != null
				|| !result.getOnly(SysMLPackage.Literals.CROSS_SUBSETTING).isEmpty()) {
			return;
		}
		Type owner = feature.getOwningType();
		if (owner == null) {
			return;
		}
		List<Feature> ends = owner.getOwnedEndFeature();
		if (ends.size() == 2) {
			Feature other = ends.get(ends.indexOf(feature) == 0 ? 1 : 0);
			result.add(SysMLPackage.Literals.CROSS_SUBSETTING, FeatureUtil.chainFeatures(other, cross));
		} else {
			Feature first = SysMLFactory.eINSTANCE.createFeature();
			FeatureUtil.addFeaturingTypesTo(first, Collections.singleton(owner));
			Feature chain = FeatureUtil.chainFeatures(first, cross);
			chain.getOwnedFeatureChaining().get(0).getOwnedRelatedElement().add(first);
			result.add(SysMLPackage.Literals.CROSS_SUBSETTING, chain);
			FeatureUtil.addOwnedCrossFeatureTypeFeaturingTo(cross);
			for (Type type : cross.getFeaturingType()) {
				FeatureTyping typing = SysMLFactory.eINSTANCE.createFeatureTyping();
				typing.setType(type);
				first.getOwnedRelationship().add(typing);
			}
		}
	}

	void removeUnnecessarySpecializations(Type type, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		List<Specialization> explicitSpecializations = type.getOwnedSpecialization().stream()
				.filter(specialization -> specialization.getSpecific() == type).toList();
		List<Type> allImplicit = result.toSpecializations().stream()
				.map(ImplicitSpecialization::generalType).toList();
		for (EClass kind : result.getKinds().toArray(EClass[]::new)) {
			List<Type> generals = new ArrayList<>(result.getOnly(kind));
			if (kind == SysMLPackage.Literals.REDEFINITION) {
				List<Type> explicitRedefinitions = explicitSpecializations.stream()
						.filter(kind::isInstance)
						.map(specialization -> basicGeneral(specialization, context))
						.filter(java.util.Objects::nonNull).toList();
				generals.removeAll(explicitRedefinitions);
			} else {
				List<Type> explicitGenerals = explicitSpecializations.stream()
						// A typed/subsetted/redefined feature can make a default of another
						// feature-specialization kind redundant. Preserve that historical
						// cross-kind behavior. A plain Specialization, however, carries no
						// feature-specialization semantics and must not suppress an inferred
						// FeatureTyping (for example on invocation expressions).
						.filter(specialization -> specialization.eClass() != SysMLPackage.Literals.SPECIALIZATION
								|| kind == SysMLPackage.Literals.SPECIALIZATION)
						.map(specialization -> basicGeneral(specialization, context))
						.filter(general -> general != null && general != type).toList();
				generals.removeIf(general -> explicitGenerals.stream()
						.anyMatch(candidate -> specializesExcluding(type, candidate, general, context))
						|| allImplicit.stream().anyMatch(candidate -> candidate != general
								&& specializesExcluding(type, candidate, general, context)));
			}
			result.remove(kind);
			generals.forEach(general -> result.add(kind, general));
		}
	}

	private static Type basicGeneral(Specialization specialization,
			ImplicitSpecializationEvaluationContext context) {
		Type general = (Type)specialization.eGet(SysMLPackage.Literals.SPECIALIZATION__GENERAL, false);
		if (general != null && general.eIsProxy()) {
			context.markUncacheable();
			return null;
		}
		return general;
	}

	private boolean specializesExcluding(Type excluded, Type subtype, Type supertype,
			ImplicitSpecializationEvaluationContext context) {
		Set<Type> visited = new HashSet<>();
		visited.add(excluded);
		return specializes(subtype, supertype, context, visited);
	}

	static boolean specializes(Type subtype, Type supertype,
			ImplicitSpecializationEvaluationContext context) {
		return specializes(subtype, supertype, context, new HashSet<>());
	}

	private static boolean specializes(Type subtype, Type supertype,
			ImplicitSpecializationEvaluationContext context, Set<Type> visited) {
		if (subtype == null || supertype == null || !visited.add(subtype)) {
			return false;
		}
		if (subtype == supertype) {
			return true;
		}
		for (Type general : directGeneralTypes(subtype, context)) {
			if (general == supertype || specializes(general, supertype, context, visited)) {
				return true;
			}
		}
		return false;
	}

	static List<Type> directGeneralTypes(Type type, ImplicitSpecializationEvaluationContext context) {
		List<Type> generals = new ArrayList<>();
		type.getOwnedSpecialization().stream()
				.map(specialization -> basicGeneral(specialization, context))
				.filter(general -> general != null && general != type).forEach(generals::add);
		for (Type general : context.generalTypes(type)) {
			if (general != type && !generals.contains(general)) {
				generals.add(general);
			}
		}
		return generals;
	}

	private boolean hasStructureType(Feature feature, ImplicitSpecializationEvaluationContext context) {
		return hasType(feature, Structure.class, context);
	}

	private boolean hasClassType(Feature feature, ImplicitSpecializationEvaluationContext context) {
		return hasType(feature, org.omg.sysml.lang.sysml.Class.class, context);
	}

	private boolean hasDataType(Feature feature, ImplicitSpecializationEvaluationContext context) {
		return hasType(feature, DataType.class, context);
	}

	private boolean hasType(Feature feature, java.lang.Class<?> kind,
			ImplicitSpecializationEvaluationContext context) {
		for (FeatureTyping typing : feature.getOwnedTyping()) {
			Type type = (Type)typing.eGet(SysMLPackage.Literals.FEATURE_TYPING__TYPE, false);
			if (type != null && type.eIsProxy()) {
				context.markUncacheable();
			} else if (kind.isInstance(type)) {
				return true;
			}
		}
		return context != null && context.evaluate(feature).stream()
				.filter(specialization -> SysMLPackage.Literals.FEATURE_TYPING
						.isSuperTypeOf(specialization.specializationKind()))
				.map(ImplicitSpecialization::generalType).anyMatch(kind::isInstance);
	}

	private boolean isSubitem(ItemUsage usage) {
		return usage.isComposite()
				&& (usage.getOwningType() instanceof ItemDefinition || usage.getOwningType() instanceof ItemUsage);
	}

	private boolean isSubobject(Feature feature, ImplicitSpecializationEvaluationContext context) {
		return feature.isComposite() && (feature.getOwningType() instanceof Structure
				|| feature.getOwningType() instanceof Feature owner && hasStructureType(owner, context));
	}

	private boolean isSuboccurrence(Feature feature, ImplicitSpecializationEvaluationContext context) {
		boolean result = feature.isComposite()
				&& (feature.getOwningType() instanceof org.omg.sysml.lang.sysml.Class
						|| feature.getOwningType() instanceof Feature owner && hasClassType(owner, context));
		if (feature instanceof OccurrenceUsage occurrence) {
			result |= occurrence.isComposite() && occurrence.getOwningType() instanceof OccurrenceUsage;
		}
		if (feature instanceof ItemUsage item && isSubitem(item)) {
			return false;
		}
		if (feature instanceof ActionUsage action && isActionOwnedComposite(action)) {
			return false;
		}
		return result;
	}

	private boolean isStructureOwnedComposite(Feature feature,
			ImplicitSpecializationEvaluationContext context) {
		return feature.isComposite() && (feature.getOwningType() instanceof Structure
				|| feature.getOwningType() instanceof Feature owner && hasStructureType(owner, context));
	}

	private boolean isBehaviorOwned(Feature feature) {
		if (feature instanceof org.omg.sysml.lang.sysml.AssertConstraintUsage) {
			return feature.getOwningType() instanceof ActionDefinition
					|| feature.getOwningType() instanceof ActionUsage;
		}
		return FeatureUtil.isPerformanceFeature(feature);
	}

	private boolean isActionOwnedComposite(Usage usage) {
		return usage.isComposite()
				&& (usage.getOwningType() instanceof ActionDefinition || usage.getOwningType() instanceof ActionUsage);
	}

	private boolean isPartOwnedComposite(Usage usage) {
		return usage.isComposite()
				&& (usage.getOwningType() instanceof PartDefinition || usage.getOwningType() instanceof PartUsage);
	}

	private boolean isPerformedAction(Usage usage) {
		return usage.getOwningType() instanceof PartDefinition || usage.getOwningType() instanceof PartUsage;
	}

	private boolean isTriggerAction(AcceptActionUsage action) {
		return action.getOwningFeatureMembership() instanceof TransitionFeatureMembership membership
				&& membership.getKind() == org.omg.sysml.lang.sysml.TransitionFeatureKind.TRIGGER;
	}

	private String mapped(Type type, String key) {
		return ImplicitGeneralizationMap.getDefaultSupertypeFor(type.getClass(), key);
	}

	private Type library(Element context, String... names) {
		return SysMLLibraryUtil.getLibraryType(context, names);
	}

	private Type library(ImplicitSpecializationResult result, Element context, String... names) {
		Type type = library(context, names);
		if (type == null && java.util.Arrays.stream(names).anyMatch(java.util.Objects::nonNull)) {
			result.markIncomplete();
		}
		return type;
	}

	private void addDefault(Type type, ImplicitSpecializationResult result, EClass kind, String key) {
		addMapped(result, type, kind, key);
	}

	private void addMapped(ImplicitSpecializationResult result, Type type, EClass kind, String key) {
		addLibrary(result, type, kind, mapped(type, key));
	}

	private void addLibrary(ImplicitSpecializationResult result, Element context, EClass kind,
			String... names) {
		result.add(kind, library(result, context, names));
	}
}
