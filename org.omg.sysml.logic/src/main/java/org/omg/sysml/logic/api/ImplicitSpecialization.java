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
package org.omg.sysml.logic.api;

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.omg.sysml.lang.sysml.Type;

/**
 * Immutable description of a specialization inferred from a SysML type.
 *
 * @param specializationKind the concrete metaclass of the inferred relationship
 * @param generalType the inferred general type
 */
public record ImplicitSpecialization(EClass specializationKind, Type generalType) {

	public ImplicitSpecialization {
		Objects.requireNonNull(specializationKind, "specializationKind");
		Objects.requireNonNull(generalType, "generalType");
	}
}
