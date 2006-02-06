/*
 * Copyright 2005 Dean Wampler. All rights reserved.
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

package org.contract4j5;

import java.util.Map;


/**
 * Encapsulate current context information for a test.
 * @author Dean Wampler
 */
public interface TestContext {
	/**
	 * @return the name of the item being tested, the class, method, or field.
	 */
	String getItemName();
	void   setItemName(String itemName);
	
	/**
	 * @return the {@link Instance} being tested. When fields are being tested, it 
	 * will be the object that contains the field. Note that it is not a "snapshot" 
	 * of some prior state, but the currently-active object.  
	 * @note a pointcut "this()" or "target()" might get used to assign this value.
	 */
	Instance getInstance ();
	void     setInstance (Instance instance);
	
	/**
	 * @return the field {@link Instance}, which is only non-null when a field is being 
	 * tested.
	 */
	Instance getField ();
	void     setField (Instance target);
	
	/**
	 * @return the arguments to the method, where applicable, or null.
	 */
	Instance[] getMethodArgs ();
	void       setMethodArgs (Instance[] methodArgs);
	
	/**
	 * @return the result of the method, where applicable, or null.
	 */
	Instance getMethodResult ();
	void     setMethodResult (Instance methodResult);
	
	/**
	 * @return the map of saved "old" values. Note that if any of them are actually
	 * references to mutable objects, there is no guarantee that they didn't change!
	 */
	Map<String, Object> getOldValuesMap();
	void                setOldValuesMap (Map<String, Object> map);
	
	/**
	 * @return the file name where the test occurs.
	 */
	String getFileName();
	void   setFileName (String fileName);
	
	/**
	 * @return the line number where the test is <em>evaluated</em>, not necessarily
	 * where it is declared.
	 */
	int  getLineNumber();
	void setLineNumber (int lineNumber);
}
