package org.cyclops.integratedmekanism.gametest.integration;

import com.google.common.collect.Sets;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.registries.MekanismChemicals;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.core.test.IntegrationBefore;
import org.cyclops.integrateddynamics.core.test.IntegrationTest;
import org.cyclops.integrateddynamics.core.test.TestHelpers;
import org.cyclops.integratedmekanism.value.ValueObjectTypeChemicalStack;
import org.cyclops.integratedscripting.api.evaluate.translation.IEvaluationExceptionFactory;
import org.cyclops.integratedscripting.evaluate.ScriptHelpers;
import org.cyclops.integratedscripting.evaluate.translation.ValueTranslators;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

/**
 * @author rubensworks
 */
public class TestValueTranslators {

    private static ValueDeseralizationContext VDC = null;
    private static Context CTX = null;
    private static IEvaluationExceptionFactory EF = ScriptHelpers.getDummyEvaluationExceptionFactory();

    @IntegrationBefore
    public void before() {
        VDC = ValueDeseralizationContextMocked.get();
        try {
            CTX = ScriptHelpers.createPopulatedContext(null, VDC);
        } catch (EvaluationException e) {
            e.printStackTrace();
        }
    }

    public static Value getJsValue(String jsString) {
        return CTX.eval("js", jsString);
    }

    @IntegrationTest
    public void testObjectChemical() throws EvaluationException {
        TestHelpers.assertEqual(ValueTranslators.REGISTRY.translateFromGraal(CTX, getJsValue("exports = { id_chemical: { id: 'mekanism:steam', amount: 1000 } }"), EF, VDC), ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 1000)), "chemical raw value");

        Value translated = ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 1000)), EF, VDC);
        TestHelpers.assertEqual(translated.hasMembers(), true, "hasMembers true");
        TestHelpers.assertEqual(translated.getMemberKeys(), Sets.newHashSet("id_chemical"), "member keys is correct");
        TestHelpers.assertEqual(translated.getMember("id_chemical").hasMembers(), true, "id_chemical hasMembers true");
        TestHelpers.assertEqual(translated.getMember("id_chemical").getMemberKeys(), Sets.newHashSet("id", "amount"), "id_chemical keys is correct");
        TestHelpers.assertEqual(translated.getMember("id_chemical").getMember("id").asString(), "mekanism:steam", "id_chemical has member type");
        TestHelpers.assertEqual(translated.getMember("id_chemical").getMember("amount").asInt(), 1000, "id_chemical has member amount");
    }

    @IntegrationTest
    public void testObjectChemicalMethods() throws EvaluationException {
        TestHelpers.assertEqual(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 1000)), EF, VDC).invokeMember("amount").asInt(), 1000, "amount can be invoked for chemical stacks");
        TestHelpers.assertEqual(ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 123)), EF, VDC).invokeMember("amount").asInt(), 123, "amount can be invoked for chemical stacks");
    }

    @IntegrationTest
    public void testGlobalFunctions() throws EvaluationException {
        Value ops = CTX.getBindings("js").getMember("idContext").getMember("ops");

        TestHelpers.assertEqual(
                ops.invokeMember("anyEquals",
                        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 10)), EF, VDC),
                        ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 20)), EF, VDC)
                ).asBoolean(),
                false,
                "anyEquals works for chemical stacks"
        );

        TestHelpers.assertEqual(
                ops.invokeMember("chemicalstackAmount", ValueTranslators.REGISTRY.translateToGraal(CTX, ValueObjectTypeChemicalStack.ValueChemicalStack.of(new ChemicalStack(MekanismChemicals.STEAM, 1000)), EF, VDC)).asInt(),
                1000,
                "chemicalstackAmount works for chemical stacks"
        );
    }

}
