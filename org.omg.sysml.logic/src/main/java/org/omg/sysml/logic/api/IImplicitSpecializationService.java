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

import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.omg.sysml.lang.sysml.Specialization;
import org.omg.sysml.lang.sysml.Type;

/** Computes and optionally materializes implicit SysML specializations. */
public interface IImplicitSpecializationService {

	/** Returns all currently inferred specializations without modifying the model. */
	List<ImplicitSpecialization> getImplicitSpecializations(Type type);

	/**
	 * Returns inferred specializations of the requested relationship kind.
	 *
	 * @param includeSubtypes whether concrete subtypes of {@code specializationKind}
	 *        are included
	 */
	List<ImplicitSpecialization> getImplicitSpecializations(Type type, EClass specializationKind,
			boolean includeSubtypes);

	/** Returns whether this service is currently evaluating the given type. */
	boolean isComputing(Type type);

	/** Inserts currently inferred specializations and returns the relationships created. */
	List<Specialization> materialize(Type type);

	/** Invalidates any cached result for the type. */
	void invalidate(Type type);
}
