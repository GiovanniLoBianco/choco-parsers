/**
 * This file is part of choco-parsers, https://github.com/chocoteam/choco-parsers
 *
 * Copyright (c) 2017, IMT Atlantique. All rights reserved.
 *
 * Licensed under the BSD 4-clause license.
 * See LICENSE file in the project root for full license information.
 */
package org.chocosolver.parser;

import org.chocosolver.pf4cs.IProblem;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Settings;

/**
 * An interface for all parsers
 * <br/>
 *
 * @author Charles Prud'homme
 * @version choco-parsers
 * @since 21/10/2014
 */
public interface IParser extends IProblem{

    boolean PRINT_LOG = true;

    /**
     * Add a parser listener
     *
     * @param listener
     */
    void addListener(ParserListener listener);

    /**
     * Remove a parser listener
     *
     * @param listener
     */
    void removeListener(ParserListener listener);

    /**
     * Declare the settings to use
     *
     * @param defaultSettings settings to consider
     */
    void defineSettings(Settings defaultSettings);

    /**
     * Create the solver
     */
    void createSolver();

    /**
     * @return a thread to execute on unexpected exit
     */
    Thread actionOnKill();

    /**
     * Get the solver
     *
     * @return solver
     */
    Model getModel();
}
