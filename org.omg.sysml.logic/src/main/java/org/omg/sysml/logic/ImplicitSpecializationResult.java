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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.logic.api.ImplicitSpecialization;

/** Mutable result used only while one service request is evaluated. */
final class ImplicitSpecializationResult {

	private final Type type;
	private final Map<EClass, List<Type>> generalTypes = new LinkedHashMap<>();
	private boolean defaultsAllowed = true;
	private boolean complete = true;

	ImplicitSpecializationResult(Type type) {
		this.type = type;
	}

	void add(EClass kind, Type general) {
		if (kind != null && general != null && general != type) {
			List<Type> generals = generalTypes.computeIfAbsent(kind, key -> new ArrayList<>());
			if (!generals.contains(general)) {
				generals.add(general);
			}
		}
	}

	void remove(EClass kind) {
		generalTypes.remove(kind);
	}

	List<Type> getOnly(EClass kind) {
		return generalTypes.getOrDefault(kind, List.of());
	}

	Collection<EClass> getKinds() {
		return generalTypes.keySet().stream()
				.sorted(Comparator.comparingInt(EClass::getClassifierID))
				.toList();
	}

	boolean containsKind(EClass kind) {
		return generalTypes.containsKey(kind);
	}

	void suppressDefaults() {
		defaultsAllowed = false;
	}

	boolean areDefaultsAllowed() {
		return defaultsAllowed;
	}

	void markIncomplete() {
		complete = false;
	}

	boolean isComplete() {
		return complete;
	}

	List<ImplicitSpecialization> toSpecializations() {
		List<ImplicitSpecialization> result = new ArrayList<>();
		for (EClass kind : getKinds()) {
			for (Type general : getOnly(kind)) {
				result.add(new ImplicitSpecialization(kind, general));
			}
		}
		return List.copyOf(result);
	}
}
