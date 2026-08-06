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
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.omg.sysml.logic.api.IImplicitSpecializationCacheAdapter;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureChaining;
import org.omg.sysml.lang.sysml.Specialization;
import org.omg.sysml.lang.sysml.SysMLFactory;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.logic.api.IImplicitSpecializationService;
import org.omg.sysml.logic.api.ImplicitSpecialization;

/** Default implementation of the implicit-specialization service. */
public class ImplicitSpecializationService implements IImplicitSpecializationService {

	private final ContextualImplicitSpecializationRules contextualRules = new ContextualImplicitSpecializationRules();
	private final ImplicitSpecializationRules rules = new ImplicitSpecializationRules();
	private final ThreadLocal<ImplicitSpecializationEvaluationContext> activeContext = new ThreadLocal<>();

	public ImplicitSpecializationService() {
	}

	@Override
	public List<ImplicitSpecialization> getImplicitSpecializations(Type type) {
		if (type == null) {
			return List.of();
		}
		ImplicitSpecializationEvaluationContext currentContext = activeContext.get();
		if (currentContext != null) {
			return currentContext.evaluate(type);
		}
		IImplicitSpecializationCacheAdapter cache = getCache(type);
		if (cache != null && !cache.isDirty()) {
			return cache.getSpecializations();
		}
		if (cache != null && !cache.beginUpdate()) {
			return cache.getSpecializations();
		}
		try {
			ImplicitSpecializationEvaluationContext context = new ImplicitSpecializationEvaluationContext(this);
			activeContext.set(context);
			List<ImplicitSpecialization> computed = context.evaluate(type);
			if (cache != null) {
				cache.update(computed);
				if (!context.isCacheable()) {
					cache.invalidate();
				}
			}
			return computed;
		} finally {
			activeContext.remove();
			if (cache != null) {
				cache.endUpdate();
			}
		}
	}

	@Override
	public List<ImplicitSpecialization> getImplicitSpecializations(Type type, EClass specializationKind,
			boolean includeSubtypes) {
		if (specializationKind == null) {
			return List.of();
		}
		return getImplicitSpecializations(type).stream()
				.filter(specialization -> includeSubtypes
						? specializationKind.isSuperTypeOf(specialization.specializationKind())
						: specializationKind == specialization.specializationKind())
				.toList();
	}

	@Override
	public boolean isComputing(Type type) {
		ImplicitSpecializationEvaluationContext context = activeContext.get();
		return context != null && context.isEvaluating(type);
	}

	void compute(Type type, ImplicitSpecializationResult result,
			ImplicitSpecializationEvaluationContext context) {
		contextualRules.apply(type, result, context);
		if (result.areDefaultsAllowed()) {
			rules.apply(type, result, context);
		}
		if (!result.isComplete()) {
			context.markUncacheable();
		}
	}

	private boolean isAlreadyOwned(Type type, ImplicitSpecialization candidate) {
		return type.getOwnedSpecialization().stream()
				.anyMatch(specialization -> candidate.specializationKind().isInstance(specialization)
						&& specialization.getSpecific() == type
						&& isSameGeneral(candidate.generalType(), specialization.getGeneral()));
	}

	private boolean isSameGeneral(Type inferredGeneral, Type ownedGeneral) {
		if (inferredGeneral == ownedGeneral) {
			return true;
		}
		// Some rules infer a detached feature chain. Materialization owns that chain,
		// while a later computation creates an equivalent detached instance.
		return inferredGeneral instanceof Feature inferredFeature
				&& ownedGeneral instanceof Feature ownedFeature
				&& inferredFeature.getOwningRelationship() == null
				&& hasSameFeatureChain(inferredFeature, ownedFeature);
	}

	private boolean hasSameFeatureChain(Feature inferredFeature, Feature ownedFeature) {
		List<Feature> inferredChain = inferredFeature.getChainingFeature();
		List<Feature> ownedChain = ownedFeature.getChainingFeature();
		if (inferredChain.isEmpty() || inferredChain.size() != ownedChain.size()) {
			return false;
		}
		for (int i = 0; i < inferredChain.size(); i++) {
			if (!isSameChainingFeature(inferredChain.get(i), ownedChain.get(i))) {
				return false;
			}
		}
		return true;
	}

	private boolean isSameChainingFeature(Feature inferredFeature, Feature ownedFeature) {
		if (inferredFeature == ownedFeature) {
			return true;
		}
		if (!(inferredFeature.getOwningRelationship() instanceof FeatureChaining)
				|| !(ownedFeature.getOwningRelationship() instanceof FeatureChaining)) {
			return false;
		}
		return inferredFeature.eClass() == ownedFeature.eClass()
				&& inferredFeature.getOwnedTyping().stream().map(typing -> typing.getType()).toList()
						.equals(ownedFeature.getOwnedTyping().stream().map(typing -> typing.getType()).toList())
				&& inferredFeature.getFeaturingType().equals(ownedFeature.getFeaturingType())
				&& (inferredFeature.getChainingFeature().isEmpty()
						|| hasSameFeatureChain(inferredFeature, ownedFeature));
	}

	@Override
	public List<Specialization> materialize(Type type) {
		// Materialization is an explicit lifecycle boundary. It must not read a value
		// cached before child transforms, nor the stable value exposed while a lazy
		// computation of the same type is still in progress.
		ImplicitSpecializationEvaluationContext context = new ImplicitSpecializationEvaluationContext(this);
		List<ImplicitSpecialization> candidates = context.evaluate(type);
		List<Specialization> created = new ArrayList<>();
		for (ImplicitSpecialization candidate : candidates) {
			if (!isAlreadyOwned(type, candidate)) {
				Specialization specialization = (Specialization)SysMLFactory.eINSTANCE
						.create(candidate.specializationKind());
				specialization.setIsImplied(true);
				specialization.setGeneral(candidate.generalType());
				specialization.setSpecific(type);
				if (candidate.generalType().getOwningRelationship() == null
						&& candidate.generalType().eResource() == null) {
					specialization.getOwnedRelatedElement().add(candidate.generalType());
				}
				type.getOwnedRelationship().add(specialization);
				created.add(specialization);
			}
		}
		invalidate(type);
		return List.copyOf(created);
	}

	@Override
	public void invalidate(Type type) {
		IImplicitSpecializationCacheAdapter cache = getCache(type);
		if (cache != null) {
			cache.invalidate();
		}
	}

	private IImplicitSpecializationCacheAdapter getCache(Type type) {
		return type.eAdapters().stream()
				.filter(IImplicitSpecializationCacheAdapter.class::isInstance)
				.map(IImplicitSpecializationCacheAdapter.class::cast)
				.findFirst()
				.orElse(null);
	}
}
