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

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.logic.api.ImplicitSpecialization;

/**
 * Request-scoped recursion guard. It is passed explicitly between rules and is
 * never stored globally or attached to model elements.
 */
final class ImplicitSpecializationEvaluationContext {

	private final ImplicitSpecializationService service;
	private final Map<Type, Entry> entries = new IdentityHashMap<>();
	private final Set<Type> metadataEvaluation = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
	private boolean cacheable = true;

	ImplicitSpecializationEvaluationContext(ImplicitSpecializationService service) {
		this.service = service;
	}

	List<ImplicitSpecialization> evaluate(Type type) {
		Entry entry = entries.get(type);
		if (entry != null) {
			return entry.result.toSpecializations();
		}
		entry = new Entry(new ImplicitSpecializationResult(type));
		entries.put(type, entry);
		service.compute(type, entry.result, this);
		return entry.result.toSpecializations();
	}

	List<Type> generalTypes(Type type) {
		return evaluate(type).stream().map(ImplicitSpecialization::generalType).toList();
	}

	boolean isEvaluating(Type type) {
		return entries.containsKey(type);
	}

	boolean enterMetadataEvaluation(Type type) {
		return metadataEvaluation.add(type);
	}

	void leaveMetadataEvaluation(Type type) {
		metadataEvaluation.remove(type);
	}

	void markUncacheable() {
		cacheable = false;
	}

	boolean isCacheable() {
		return cacheable;
	}

	private record Entry(ImplicitSpecializationResult result) {
	}
}
