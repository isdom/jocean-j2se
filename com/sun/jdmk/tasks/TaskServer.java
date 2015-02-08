/* 
 * @(#)file      TaskServer.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.10
 * @(#)lastedit  07/03/08
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
 */ 

// NPCTE fix for bugId 4510777, esc 532372, MR October 2001 
// file TaskServer.java created for this bug fix


package org.jocean.j2se.jdmk.tasks;

/**
 * This interface is implemented by objects that are able to execute
 * tasks. Whether the task is executed in the client thread or in another
 * thread depends on the TaskServer implementation.
 *
 * @see org.jocean.j2se.jdmk.tasks.Task
 *
 * @since Java DMK 5.0
 **/
public interface TaskServer {
    /**
     * Submit a task to be executed.
     * Once a task is submitted, it is guaranteed that either
     * {@link org.jocean.j2se.jdmk.tasks.Task#run() task.run()} or 
     * {@link org.jocean.j2se.jdmk.tasks.Task#cancel() task.cancel()} will be called.
     * <p>Whether the task is executed in the client thread (e.g. 
     * <code>public void submitTask(Task task) { task.run(); }</code>) or in 
     * another thread (e.g. <code>
     * public void submitTask(Task task) { new Thrad(task).start(); }</code>) 
     * depends on the TaskServer implementation.
     * @param task The task to be executed.
     **/
    public void submitTask(Task task);
}
