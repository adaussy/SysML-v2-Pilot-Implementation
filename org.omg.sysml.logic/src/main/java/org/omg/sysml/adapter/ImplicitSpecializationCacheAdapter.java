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
package org.omg.sysml.adapter;

import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.logic.api.IImplicitSpecializationCacheAdapter;
import org.omg.sysml.logic.api.ImplicitSpecialization;

/**
 * Per-type adapter containing only the cache of implicit specializations.
 */
public final class ImplicitSpecializationCacheAdapter extends AdapterImpl
		implements IImplicitSpecializationCacheAdapter {

	private List<ImplicitSpecialization> specializations = List.of();
	private boolean dirty = true;
	private boolean updating;
	private boolean invalidatedDuringUpdate;

	@Override
	public boolean isAdapterForType(Object type) {
		return type == ImplicitSpecializationCacheAdapter.class;
	}

	public static ImplicitSpecializationCacheAdapter get(Type type) {
		if (type == null) {
			return null;
		}
		Optional<ImplicitSpecializationCacheAdapter> existing = type.eAdapters().stream()
				.filter(ImplicitSpecializationCacheAdapter.class::isInstance)
				.map(ImplicitSpecializationCacheAdapter.class::cast)
				.findFirst();
		return existing.orElse(null);
	}

	public static ImplicitSpecializationCacheAdapter installOn(Type type) {
		if (type == null) {
			return null;
		}
		ResourceSet resourceSet = type.eResource() == null ? null : type.eResource().getResourceSet();
		ImplicitSpecializationCacheInvalidationAdapter invalidationAdapter = null;
		if (resourceSet != null) {
			invalidationAdapter = ImplicitSpecializationCacheInvalidationAdapter.installOn(resourceSet);
		}
		ImplicitSpecializationCacheAdapter existing = get(type);
		if (existing != null) {
			if (invalidationAdapter != null) {
				invalidationAdapter.register(existing);
			}
			return existing;
		}
		ImplicitSpecializationCacheAdapter adapter = new ImplicitSpecializationCacheAdapter();
		type.eAdapters().add(adapter);
		if (invalidationAdapter != null) {
			invalidationAdapter.register(adapter);
		}
		return adapter;
	}

	public static void removeFrom(Type type) {
		ImplicitSpecializationCacheAdapter adapter = get(type);
		if (adapter != null) {
			if (type.eResource() != null) {
				ImplicitSpecializationCacheInvalidationAdapter invalidationAdapter =
						ImplicitSpecializationCacheInvalidationAdapter.get(type.eResource().getResourceSet());
				if (invalidationAdapter != null) {
					invalidationAdapter.unregister(adapter);
				}
			}
			type.eAdapters().remove(adapter);
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public List<ImplicitSpecialization> getSpecializations() {
		return specializations;
	}

	@Override
	public boolean beginUpdate() {
		if (updating) {
			return false;
		}
		updating = true;
		invalidatedDuringUpdate = false;
		watchForInvalidation();
		return true;
	}

	@Override
	public void endUpdate() {
		updating = false;
	}

	@Override
	public void update(List<ImplicitSpecialization> newSpecializations) {
		specializations = List.copyOf(newSpecializations);
		dirty = invalidatedDuringUpdate;
		if (!dirty) {
			watchForInvalidation();
		}
	}

	private void watchForInvalidation() {
		if (getTarget() instanceof Type type && type.eResource() != null) {
			ImplicitSpecializationCacheInvalidationAdapter invalidationAdapter =
					ImplicitSpecializationCacheInvalidationAdapter.get(type.eResource().getResourceSet());
			if (invalidationAdapter != null) {
				invalidationAdapter.watch(this);
			}
		}
	}

	@Override
	public void invalidate() {
		dirty = true;
		if (updating) {
			invalidatedDuringUpdate = true;
		}
	}
}
