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
package org.omg.sysml.util;

import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.logic.ImplicitSpecializationService;
import org.omg.sysml.logic.api.IImplicitSpecializationService;

/** Resolves the implicit-specialization service appropriate for a model resource. */
public final class ImplicitSpecializationUtil {

	@FunctionalInterface
	public interface ProviderLookup {
		IImplicitSpecializationService get(Type type);
	}

	private static final IImplicitSpecializationService DEFAULT_SERVICE = new ImplicitSpecializationService();

	private static volatile ProviderLookup providerLookup;

	private ImplicitSpecializationUtil() {
	}

	public static void setProviderLookup(ProviderLookup lookup) {
		providerLookup = lookup;
	}

	public static IImplicitSpecializationService getService(Type context) {
		ProviderLookup lookup = providerLookup;
		if (lookup != null) {
			try {
				IImplicitSpecializationService service = lookup.get(context);
				if (service != null) {
					return service;
				}
			} catch (RuntimeException exception) {
				// Fall back to the standalone service when no contextual provider can
				// be resolved (for example for a detached model element).
			}
		}
		return DEFAULT_SERVICE;
	}
}
