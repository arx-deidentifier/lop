/*
 * LOP: Low Overhead Profiler
 * Copyright (C) 2014 Florian Kohlmayer, Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.deidentifier.arx.profiler;

/**
 * This class implements a simplistic (low overhead) profiler
 *
 * @author Prasser, Kohlmayer
 */
public class Profiler {

    /** Returns the singletom instance*/
    public static Profiler get(){
        return instance;
    }
    
    /** Instance*/
    private LowOverheadProfiler profiler = LowOverheadProfiler.get();
    
    /** Instance*/
    private static Profiler instance = new Profiler();
    
    /**
     * Resets the profiler
     * @throws ClassNotFoundException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    public void reset() throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException{
        profiler.reset();
    }
}
