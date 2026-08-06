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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.omg.sysml.logic.api.IImplicitSpecializationCacheAdapter;

/**
 * Observes one resource set and invalidates element-local caches after model
 * changes.
 *
 * <p>The invalidation is deliberately conservative. Implicit-specialization
 * rules have local, contextual and transitive dependencies, so every semantic
 * model change invalidates every installed specialization cache in the resource
 * set. Values are recomputed lazily by the service when they are next read.</p>
 */
public final class ImplicitSpecializationCacheInvalidationAdapter extends EContentAdapter {

	private final Set<IImplicitSpecializationCacheAdapter> observedCaches =
			Collections.newSetFromMap(new WeakHashMap<>());
	private boolean invalidating;

	private ImplicitSpecializationCacheInvalidationAdapter() {
	}

	@Override
	public boolean isAdapterForType(Object type) {
		return type == ImplicitSpecializationCacheInvalidationAdapter.class;
	}

	public static ImplicitSpecializationCacheInvalidationAdapter get(ResourceSet resourceSet) {
		if (resourceSet == null) {
			return null;
		}
		Optional<ImplicitSpecializationCacheInvalidationAdapter> existing = resourceSet.eAdapters().stream()
				.filter(ImplicitSpecializationCacheInvalidationAdapter.class::isInstance)
				.map(ImplicitSpecializationCacheInvalidationAdapter.class::cast)
				.findFirst();
		return existing.orElse(null);
	}

	public static ImplicitSpecializationCacheInvalidationAdapter installOn(ResourceSet resourceSet) {
		ImplicitSpecializationCacheInvalidationAdapter existing = get(resourceSet);
		if (existing != null || resourceSet == null) {
			return existing;
		}
		ImplicitSpecializationCacheInvalidationAdapter adapter =
				new ImplicitSpecializationCacheInvalidationAdapter();
		resourceSet.eAdapters().add(adapter);
		return adapter;
	}

	public static void removeFrom(ResourceSet resourceSet) {
		ImplicitSpecializationCacheInvalidationAdapter adapter = get(resourceSet);
		if (adapter != null) {
			resourceSet.eAdapters().remove(adapter);
		}
	}

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		if (isSemanticModelChange(notification)) {
			invalidateAllCaches();
		}
	}

	private boolean isSemanticModelChange(Notification notification) {
		if (notification.isTouch() || notification.getEventType() == Notification.REMOVING_ADAPTER) {
			return false;
		}
		return switch (notification.getEventType()) {
			case Notification.SET, Notification.UNSET, Notification.ADD, Notification.REMOVE,
					Notification.ADD_MANY, Notification.REMOVE_MANY, Notification.MOVE,
					Notification.RESOLVE ->
						notification.getNotifier() instanceof EObject
							|| notification.getNotifier() instanceof Resource
							|| notification.getNotifier() instanceof ResourceSet;
			default -> false;
		};
	}

	void register(IImplicitSpecializationCacheAdapter cache) {
		if (!cache.isDirty()) {
			observedCaches.add(cache);
		}
	}

	void watch(IImplicitSpecializationCacheAdapter cache) {
		observedCaches.add(cache);
	}

	void unregister(IImplicitSpecializationCacheAdapter cache) {
		observedCaches.remove(cache);
	}

	/** Invalidates all caches currently attached to elements in the resource set. */
	public void invalidateAllCaches() {
		if (invalidating || observedCaches.isEmpty()) {
			return;
		}
		invalidating = true;
		try {
			Set<IImplicitSpecializationCacheAdapter> cachesToInvalidate = Set.copyOf(observedCaches);
			observedCaches.clear();
			cachesToInvalidate.forEach(this::invalidateCache);
		} finally {
			invalidating = false;
		}
	}

	private void invalidateCache(IImplicitSpecializationCacheAdapter cache) {
		cache.invalidate();
	}
}
