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
package org.omg.sysml.xpect.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.junit.Test;
import org.omg.sysml.adapter.ImplicitSpecializationCacheAdapter;
import org.omg.sysml.adapter.ImplicitSpecializationCacheInvalidationAdapter;
import org.omg.sysml.lang.sysml.Namespace;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.lang.sysml.Type;
import org.omg.sysml.logic.ImplicitSpecializationService;
import org.omg.sysml.logic.SysMLLogicStandaloneSetup;
import org.omg.sysml.logic.api.IImplicitSpecializationCacheAdapter;
import org.omg.sysml.util.ImplicitSpecializationUtil;
import org.omg.sysml.xtext.SysMLStandaloneSetup;

import com.google.inject.Injector;

public class ImplicitSpecializationXtextTest {

	@Test
	public void usesLocalCacheForParsedModel() throws Exception {
		SysMLPackage.eINSTANCE.eClass();
		SysMLLogicStandaloneSetup.doSetup();
		Injector injector = new SysMLStandaloneSetup().createInjectorAndDoEMFRegistration();
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		XtextResource resource = (XtextResource)resourceSet.createResource(URI.createURI("implicit-specialization.sysml"));
		resource.load(new ByteArrayInputStream("part def Vehicle;".getBytes(StandardCharsets.UTF_8)), Map.of());

		assertTrue(resource.getErrors().toString(), resource.getErrors().isEmpty());
		Namespace root = (Namespace)resource.getContents().get(0);
		Type parsedType = root.getOwnedMember().stream()
				.filter(Type.class::isInstance)
				.map(Type.class::cast)
				.findFirst()
				.orElseThrow();

		assertTrue(ImplicitSpecializationUtil.getService(parsedType)
				instanceof ImplicitSpecializationService);
		ImplicitSpecializationUtil.getService(parsedType).getImplicitSpecializations(parsedType);
		IImplicitSpecializationCacheAdapter cache = ImplicitSpecializationCacheAdapter.get(parsedType);
		assertNotNull(cache);
		assertFalse(cache.isDirty());
		assertEquals(1, resourceSet.eAdapters().stream()
				.filter(ImplicitSpecializationCacheInvalidationAdapter.class::isInstance)
				.count());

		parsedType.setDeclaredName("ChangedVehicle");
		assertTrue(cache.isDirty());
		ImplicitSpecializationUtil.getService(parsedType).getImplicitSpecializations(parsedType);
		assertFalse(cache.isDirty());
		assertTrue(parsedType.getOwnedSpecialization().isEmpty());
	}
}
