/*
 * @(#)file      ConnectorAddress.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.14
 * @(#)lastedit      07/03/08
 *
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the 
 * LEGAL_NOTICES folder that accompanied this code. See the License for the 
 * specific language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 * 
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 *       "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 * 
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 * 
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 * 
 *
 */


package org.jocean.j2se.jdmk.comm;



/**
 * Interface which all protocol-specific address classes have
 * to implement. It only identifies the type of connector to be used
 * in order to communicate with the agent. This may be used by the connector
 * to check that the address used as an argument of its connect method
 * is of the appropriate type.
 *
 * @deprecated The JMX Remote API should be used in preference to the
 * legacy Java DMK connectors.  This interface may be removed in a
 * future version of Java DMK.  See {@link
 * org.jocean.j2se.jdmk.comm.JdmkLegacyConnector}.
 *
 */

public interface ConnectorAddress extends java.io.Serializable {

    /**
     * Returns a string indicating the type of Connector to use in
     * order to establish the manager-agent communication. The exact semantics
     * of the string is implementation-dependent. For example, it may identify the type
     * of protocol used to support the communication.
     */
    public String getConnectorType();

}
