/*
 * TASE
 * Copyright (C) 2017
 *
 * TASE is free software, licensed under version 3 of the GNU Affero General Public License.
 *
 */

package lbr.tase;

public class Native {

    static
    {
        System.loadLibrary("app");
    }

    native public static void killTor();

}
