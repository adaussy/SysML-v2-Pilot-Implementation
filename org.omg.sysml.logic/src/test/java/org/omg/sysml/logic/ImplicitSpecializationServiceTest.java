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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.sysml.adapter.ImplicitSpecializationCacheAdapter;
import org.omg.sysml.adapter.ImplicitSpecializationCacheInvalidationAdapter;
import org.omg.sysml.lang.sysml.Class;
import org.omg.sysml.lang.sysml.Connector;
import org.omg.sysml.lang.sysml.Expression;
import org.omg.sysml.lang.sysml.Feature;
import org.omg.sysml.lang.sysml.FeatureChainExpression;
import org.omg.sysml.lang.sysml.FeatureDirectionKind;
import org.omg.sysml.lang.sysml.FeatureTyping;
import org.omg.sysml.lang.sysml.Function;
import org.omg.sysml.lang.sysml.InvocationExpression;
import org.omg.sysml.lang.sysml.SysMLFactory;
import org.omg.sysml.lang.sysml.SysMLPackage;
import org.omg.sysml.logic.api.IImplicitSpecializationCacheAdapter;
import org.omg.sysml.logic.api.IImplicitSpecializationService;
import org.omg.sysml.logic.api.ImplicitSpecialization;
import org.omg.sysml.util.ConnectorUtil;
import org.omg.sysml.util.EvaluationUtil;
import org.omg.sysml.util.ImplicitSpecializationUtil;
import org.omg.sysml.util.SysMLLibraryUtil;
import org.omg.sysml.util.TypeUtil;

public class ImplicitSpecializationServiceTest {

	@BeforeClass
	public static void initializeModel() {
		SysMLLogicStandaloneSetup.doSetup();
		SysMLPackage.eINSTANCE.eClass();
	}

	@After
	public void resetLookups() {
		ImplicitSpecializationUtil.setProviderLookup(null);
		SysMLLibraryUtil.setProviderLookup(null);
	}

	@Test
	public void computesThroughMetamodelApiWithoutInstallingCache() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		AtomicReference<Class> generalReference = new AtomicReference<>();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> generalReference.get(), service);
		Class general = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(general);
		Class specific = SysMLFactory.eINSTANCE.createClass();

		assertEquals(0, specific.getOwnedRelationship().size());
		assertEquals(1, TypeUtil.getImplicitGeneralTypesFor(specific).size());
		assertSame(general, TypeUtil.getImplicitGeneralTypesFor(specific).get(0));
		assertTrue(service.getImplicitSpecializations(specific, SysMLPackage.Literals.SUBCLASSIFICATION, false)
				.stream().map(ImplicitSpecialization::generalType).anyMatch(general::equals));
		assertEquals(0, specific.getOwnedRelationship().size());
		assertFalse(specific.eAdapters().stream().anyMatch(ImplicitSpecializationCacheAdapter.class::isInstance));
	}

	@Test
	public void adapterCacheIsOptionalAndExplicitlyInvalidated() {
		AtomicInteger resolutions = new AtomicInteger();
		AtomicReference<Class> generalReference = new AtomicReference<>();
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> {
			resolutions.incrementAndGet();
			return generalReference.get();
		}, service);
		Class general = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(general);
		Class specific = SysMLFactory.eINSTANCE.createClass();
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		ResourceImpl resource = new ResourceImpl(URI.createURI("cache-invalidation.sysml"));
		resourceSet.getResources().add(resource);
		resource.getContents().add(specific);
		ImplicitSpecializationCacheAdapter cache = ImplicitSpecializationCacheAdapter.installOn(specific);
		assertSame(ImplicitSpecializationCacheInvalidationAdapter.get(resourceSet),
				ImplicitSpecializationCacheInvalidationAdapter.installOn(resourceSet));

		service.getImplicitSpecializations(specific);
		int afterFirstRead = resolutions.get();
		service.getImplicitSpecializations(specific);

		assertEquals(afterFirstRead, resolutions.get());
		assertFalse(cache.isDirty());

		service.invalidate(specific);
		service.getImplicitSpecializations(specific);
		assertTrue(resolutions.get() > afterFirstRead);

		Class updatedGeneral = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(updatedGeneral);
		specific.setDeclaredName("changed");
		assertTrue(cache.isDirty());
		assertSame(updatedGeneral, service.getImplicitSpecializations(specific).get(0).generalType());
	}

	@Test
	public void missingLibraryTypeIsNotCachedAsAStableResult() {
		AtomicReference<Class> generalReference = new AtomicReference<>();
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> generalReference.get(), service);
		Class specific = SysMLFactory.eINSTANCE.createClass();
		ImplicitSpecializationCacheAdapter cache = ImplicitSpecializationCacheAdapter.installOn(specific);

		assertTrue(service.getImplicitSpecializations(specific).isEmpty());
		assertTrue(cache.isDirty());

		Class general = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(general);
		assertSame(general, service.getImplicitSpecializations(specific).get(0).generalType());
		assertFalse(cache.isDirty());
	}

	@Test
	public void resourceSetAdapterInvalidatesCachesAfterNestedModelChange() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		AtomicInteger resolutions = new AtomicInteger();
		AtomicReference<Class> generalReference = new AtomicReference<>();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> {
			resolutions.incrementAndGet();
			return generalReference.get();
		}, service);
		Class initialGeneral = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(initialGeneral);
		Feature specific = SysMLFactory.eINSTANCE.createFeature();
		FeatureTyping typing = SysMLFactory.eINSTANCE.createFeatureTyping();
		specific.getOwnedRelationship().add(typing);

		ResourceSetImpl resourceSet = new ResourceSetImpl();
		ResourceImpl resource = new ResourceImpl(URI.createURI("nested-cache-invalidation.sysml"));
		resourceSet.getResources().add(resource);
		resource.getContents().add(specific);
		ImplicitSpecializationCacheAdapter cache = ImplicitSpecializationCacheAdapter.installOn(specific);

		service.getImplicitSpecializations(specific);
		int afterFirstRead = resolutions.get();
		assertFalse(cache.isDirty());

		Class updatedGeneral = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(updatedGeneral);
		typing.setType(updatedGeneral);

		assertTrue(cache.isDirty());
		assertNotNull(ImplicitSpecializationCacheInvalidationAdapter.get(resourceSet));
		service.getImplicitSpecializations(specific);
		assertTrue(resolutions.get() > afterFirstRead);
	}

	@Test
	public void modelChangeDuringComputationKeepsCacheDirty() {
		Class specific = SysMLFactory.eINSTANCE.createClass();
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		ResourceImpl resource = new ResourceImpl(URI.createURI("in-progress-cache-invalidation.sysml"));
		resourceSet.getResources().add(resource);
		resource.getContents().add(specific);
		ImplicitSpecializationCacheAdapter cache = ImplicitSpecializationCacheAdapter.installOn(specific);

		assertTrue(cache.beginUpdate());
		specific.setDeclaredName("changed-during-computation");
		cache.update(List.of());
		cache.endUpdate();

		assertTrue(cache.isDirty());
	}

	@Test
	public void evaluationContextStopsReentrantComputationWithoutCache() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		Class specific = SysMLFactory.eINSTANCE.createClass();
		Class general = SysMLFactory.eINSTANCE.createClass();
		AtomicInteger nestedComputations = new AtomicInteger();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> {
			nestedComputations.incrementAndGet();
			assertTrue(service.isComputing(specific));
			assertTrue(service.getImplicitSpecializations(specific).isEmpty());
			return general;
		}, service);

		List<ImplicitSpecialization> specializations = service.getImplicitSpecializations(specific);

		assertEquals(1, specializations.size());
		assertSame(general, specializations.get(0).generalType());
		assertFalse(service.isComputing(specific));
		assertFalse(specific.eAdapters().stream()
				.anyMatch(IImplicitSpecializationCacheAdapter.class::isInstance));
		assertTrue(nestedComputations.get() > 0);
	}

	@Test
	public void materializationIsExplicitAndIdempotent() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		AtomicReference<Class> generalReference = new AtomicReference<>();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> generalReference.get(), service);
		Class general = SysMLFactory.eINSTANCE.createClass();
		generalReference.set(general);
		Class specific = SysMLFactory.eINSTANCE.createClass();

		assertEquals(1, service.getImplicitSpecializations(specific).size());
		assertTrue(specific.getOwnedSpecialization().isEmpty());
		assertEquals(1, service.materialize(specific).size());
		assertEquals(1, specific.getOwnedSpecialization().size());
		assertTrue(specific.getOwnedSpecialization().get(0).isImplied());
		assertSame(general, specific.getOwnedSpecialization().get(0).getGeneral());

		assertTrue(service.materialize(specific).isEmpty());
		assertEquals(1, specific.getOwnedSpecialization().size());
	}

	@Test
	public void materializationOfInferredFeatureChainIsIdempotent() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> null, service);
		FeatureChainExpression expression = SysMLFactory.eINSTANCE.createFeatureChainExpression();
		Feature sourceParameter = SysMLFactory.eINSTANCE.createFeature();
		sourceParameter.setDirection(FeatureDirectionKind.IN);
		TypeUtil.addOwnedFeatureTo(expression, sourceParameter);
		Feature sourceTarget = SysMLFactory.eINSTANCE.createFeature();
		TypeUtil.addOwnedFeatureTo(sourceParameter, sourceTarget);
		TypeUtil.addResultParameterTo(expression);
		Feature result = expression.getResult();

		assertEquals(1, service.materialize(result).size());
		assertTrue(service.materialize(result).isEmpty());
		assertEquals(1, result.getOwnedSpecialization().size());
	}

	@Test
	public void genericSpecializationDoesNotSuppressImplicitFeatureTyping() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> null, service);
		Function function = SysMLFactory.eINSTANCE.createFunction();
		InvocationExpression invocation = EvaluationUtil.createInvocationOf(function);

		List<ImplicitSpecialization> typings = service.getImplicitSpecializations(
				invocation, SysMLPackage.Literals.FEATURE_TYPING, false);

		assertEquals(1, typings.size());
		assertSame(function, typings.get(0).generalType());
	}

	@Test
	public void computesContextualConnectorEndSpecializationWithoutTransformingConnector() {
		IImplicitSpecializationService service = new ImplicitSpecializationService();
		SysMLLogicStandaloneSetup.doSetup((context, name) -> null, service);
		Connector connector = SysMLFactory.eINSTANCE.createConnector();
		Feature end = ConnectorUtil.addConnectorEndTo(connector, null);
		Expression expression = SysMLFactory.eINSTANCE.createExpression();
		TypeUtil.addOwnedFeatureTo(end, expression);
		TypeUtil.addResultParameterTo(expression);
		Feature result = expression.getResult();

		assertFalse(org.omg.sysml.util.ElementUtil.isTransformed(connector));
		assertEquals(1, service.getImplicitSpecializations(
				end, SysMLPackage.Literals.SUBSETTING, false).size());
		assertSame(result, service.getImplicitSpecializations(
				end, SysMLPackage.Literals.SUBSETTING, false).get(0).generalType());
		assertTrue(end.getOwnedSpecialization().isEmpty());
	}
}
