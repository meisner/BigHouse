/**
 * Copyright (c) 2011 The Regents of The University of Michigan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met: redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer;
 * redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution;
 * neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author: David Meisner (meisner@umich.edu)
 *
 */
package core;

/**
 * Contains helper functions for debugging the simulator.
 *
 * @author David Meisner (meisner@umich.edu)
 */
public final class Sim {

    /**
     * The default debug level of the simulator.
     */
    private static final int DEFAULT_DEBUG_LEVEL = 5;

    /**
     * The current debug level of the simulator.
     */
    private static int debugLevel = DEFAULT_DEBUG_LEVEL;

    /**
     * Private constructor which prevents instantiation.
     */
    private Sim() {
        throw new UnsupportedOperationException();
    }

    /**
     * Prints the banner for the simulator.
     */
    public static void printBanner() {
        String banner =
    "Copyright (c) 2011 The Regents of The University of Michigan\n"
    + "All rights reserved.\n"
    + "\n"
    + "Redistribution and use in source and binary forms, with or without\n"
    + "modification, are permitted provided that the following conditions are\n"
    + "met: redistributions of source code must retain the above copyright\n"
    + "notice, this list of conditions and the following disclaimer;\n"
    + "redistributions in binary form must reproduce the above copyright\n"
    + "notice, this list of conditions and the following disclaimer in the\n"
    + "documentation and/or other materials provided with the distribution;\n"
    + "neither the name of the copyright holders nor the names of its\n"
    + "contributors may be used to endorse or promote products derived from\n"
    + "this software without specific prior written permission.\n"
    + ""
    + "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n"
    + "\"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT\n"
    + "LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR\n"
    + "A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT\n"
    + "OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,\n"
    + "SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT\n"
    + "LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,\n"
    + "DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY\n"
    + "THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT\n"
    + "(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n"
    + "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n"
    + "\n\n"
    + "The BigHouse Simulator\n"
    + "www.eecs.umich.edu/bighouse\n"
    + "Author: David Meisner (meisner@umich.edu)\n";

        System.out.println(banner);
    }

    /**
     * Set the level of debugging.
     * Higher levels of debugging prints more information.
     * @param level - The level of debugging to set the simulator to
     */
    public static void setDebugLevel(final int level) {
        Sim.debugLevel = level;
    }

    /**
     * Get the current debug level.
     * @return the current debug level
     */
    public static int getDebugLevel() {
        return Sim.debugLevel;
    }

    /**
     * Prints a debug message if the debug level is at
     * or above the provided threshold.
     * @param levelThreshold - the level the debug level must be
     * at to print this message
     * @param message - the message to print
     */
    public static void debug(final int levelThreshold, final String message) {
        if (levelThreshold <= Sim.debugLevel) {
            System.out.println(message);
        }
    }

    /**
     * Prints a debug message if the debug level is at or above
     * the provided threshold and annotates the time the message occurred.
     * @param levelThreshold - the level the debug level must be
     * at to print this message
     * @param time - the time (in simulation time) the debug is printed
     * @param message - the message to print
     */
    public static void debug(final int levelThreshold,
                             final double time,
                             final String message) {
        if (levelThreshold <= Sim.debugLevel) {
            debug(levelThreshold, "[" + time + "] " + message);
        }
    }

    /**
     * Prints a fatal error and ends the simulation.
     * @param message - the fatal error to print
     */
    public static void fatalError(final String message) {
        System.out.println(message);
        throw new RuntimeException();
    }

}
