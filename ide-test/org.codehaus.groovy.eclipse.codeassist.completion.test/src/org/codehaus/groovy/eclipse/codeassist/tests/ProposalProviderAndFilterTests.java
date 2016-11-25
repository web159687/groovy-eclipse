/*
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse.codeassist.tests;

import junit.framework.Test;

import org.codehaus.groovy.eclipse.codeassist.completion.mock.MockProposalFilter1;
import org.codehaus.groovy.eclipse.codeassist.completion.mock.MockProposalFilter2;
import org.codehaus.groovy.eclipse.codeassist.completion.mock.MockProposalProvider1;
import org.codehaus.groovy.eclipse.codeassist.completion.mock.MockProposalProvider2;
import org.codehaus.groovy.eclipse.codeassist.requestor.GroovyCompletionProposalComputer;
import org.codehaus.groovy.eclipse.test.EclipseTestSetup;
import org.codehaus.groovy.eclipse.test.ui.Extender1;
import org.codehaus.groovy.eclipse.test.ui.Extender2;
import org.eclipse.jdt.core.ICompilationUnit;

/**
 * Tests for {@link IProposalFilter} and {@link IProposalProvider}
 * @author Andrew Eisenberg
 * @created Aug 31, 2010
 */
public final class ProposalProviderAndFilterTests extends CompletionTestCase {

    public static Test suite() {
        return newTestSuite(ProposalProviderAndFilterTests.class);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        MockProposalFilter1.reset();
        MockProposalFilter2.reset();
        MockProposalProvider1.reset();
        MockProposalProvider2.reset();
    }

    public void testProvidersAndFilters1() throws Exception {
        EclipseTestSetup.addNature(Extender2.NATURE2);

        String contents = "println th";
        ICompilationUnit unit = create(contents);
        // perform a dummy content assist
        performContentAssist(unit, getIndexOf(contents, " th"), GroovyCompletionProposalComputer.class);

        // now see which proposals and filters have been accessed
        assertFalse("MockProposalProvider1 should not have been called", MockProposalProvider1.wasProviderCalled());
        assertFalse("MockProposalFilter1 should not have been called", MockProposalFilter1.wasFilterCalled());
        assertTrue("MockProposalProvider2 should have been called", MockProposalProvider2.wasProviderCalled());
        assertTrue("MockProposalFilter2 should have been called", MockProposalFilter2.wasFilterCalled());
    }
    public void testProvidersAndFilters2() throws Exception {
        EclipseTestSetup.addNature(Extender1.NATURE1);
        EclipseTestSetup.addNature(Extender2.NATURE2);

        String contents = "println th";
        ICompilationUnit unit = create(contents);
        // perform a dummy content assist
        performContentAssist(unit, getIndexOf(contents, " th"), GroovyCompletionProposalComputer.class);

        // now see which proposals and filters have been accessed
        assertTrue("MockProposalProvider1 should have been called", MockProposalProvider1.wasProviderCalled());
        assertTrue("MockProposalFilter1 should have been called", MockProposalFilter1.wasFilterCalled());
        assertTrue("MockProposalProvider2 should have been called", MockProposalProvider2.wasProviderCalled());
        assertTrue("MockProposalFilter2 should have been called", MockProposalFilter2.wasFilterCalled());
    }
}
