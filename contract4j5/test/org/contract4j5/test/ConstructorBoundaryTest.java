/*
 * Copyright 2005, 2006 Dean Wampler. All rights reserved.
 * http://www.aspectprogramming.com
 *
 * Licensed under the Eclipse Public License - v 1.0; you may not use this
 * software except in compliance with the License. You may obtain a copy of the 
 * License at
 *
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 * A copy is also included with this distribution. See the "LICENSE" file.
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Dean Wampler <mailto:dean@aspectprogramming.com>
 */

package org.contract4j5.test;

import junit.framework.TestCase;

import org.contract4j5.Contract;
import org.contract4j5.ContractError;
import org.contract4j5.Post;
import org.contract4j5.Pre;
import org.contract4j5.aspects.Contract4J;
import org.contract4j5.configurator.Configurator;
import org.contract4j5.configurator.test.ConfiguratorForTesting;

public class ConstructorBoundaryTest extends TestCase {
	@Contract
	public static class ConstructorBoundaryWithDefaultExpr {
		public String name = null;  // public to help tests below
		public int i = 0;
		
		@Pre @Post
		public ConstructorBoundaryWithDefaultExpr (String name, int i) {
			this.name = name;
			this.i = i;
		}
	}
	
	@Contract
	public static class ConstructorBoundaryWithDefinedExpr {
		// Even though the fields are public, Jexl still requires get/set methods!
		public String name = null;
		public String getName() { return name; }
		public int i = 0;
		public int getI() { return i; }
		
		//@Pre ("$args[0] != null && $args[1] > 0")
		@Pre ("name != null && i > 0")
		@Post ("$this.name.length() > 0 && $this.i > 1")
		public ConstructorBoundaryWithDefinedExpr (String name, int i) {
			this.name = name;
			this.i = i;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Configurator c = new ConfiguratorForTesting();
		c.configure();
		Contract4J.getContractEnforcer().getExpressionInterpreter().setTreatEmptyTestExpressionAsValidTest(false);
	}
	
	public void testCtorWithDefaultPrePostFail() {
		try {
			new ConstructorBoundaryWithDefaultExpr("foo", 0);
			fail("testCtorWithDefaultPrePostPass");  // should fail because the default @Post test is "", which is disallowed.
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefaultPostPass() {
		Contract4J.getContractEnforcer().getExpressionInterpreter().setTreatEmptyTestExpressionAsValidTest(true);
		try {
			ConstructorBoundaryWithDefaultExpr t = 
				new ConstructorBoundaryWithDefaultExpr("foo", 0);
			assertEquals(0, t.i);  
			assertEquals("foo", t.name);  
		} catch (ContractError ce) {
			fail(ce.toString());  
		}
	}
	
	public void testCtorWithDefaultPreFail1() {
		try {
			new ConstructorBoundaryWithDefaultExpr(null, 1);
			fail();  
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefaultPreFail2() {
		try {
			new ConstructorBoundaryWithDefaultExpr("foo", 0);
			fail();  
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefaultPrePass() {
		Contract4J.getContractEnforcer().getExpressionInterpreter().setTreatEmptyTestExpressionAsValidTest(true);
		try {
			new ConstructorBoundaryWithDefaultExpr("foo", 2);
		} catch (ContractError ce) {
			fail(ce.toString());  
		}
	}

	public void testCtorWithDefinedPreFail1() {
		try {
			new ConstructorBoundaryWithDefinedExpr(null, 2);
			fail();  
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefinedPreFail2() {
		try {
			new ConstructorBoundaryWithDefinedExpr("foo", 0);
			fail();  
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefinedPostFail1() {
		try {
			new ConstructorBoundaryWithDefinedExpr("", 2);
			fail();  
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefinedPostFail2() {
		try {
			new ConstructorBoundaryWithDefinedExpr("foo", 1);
			fail();  
		} catch (ContractError ce) {
		}
	}
	public void testCtorWithDefinedPostPass() {
		try {
			new ConstructorBoundaryWithDefinedExpr("foo", 2);
		} catch (ContractError ce) {
			fail(ce.toString());  
		}
	}
}
