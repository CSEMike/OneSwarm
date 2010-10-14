package org.gudy.azureus2.core3.predicate;

/*
 * Created on 5-Mar-2005
 * Created by James Yeh
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;

/**
 * <p>
 * AllPredicate is a composite of Predicables. Upon evaluation, True will only be returned
 * if all of the decorated Predicables evaluate to True.
 * </p>
 * @version 1.0
 * @author James Yeh
 */
public final class AllPredicate implements Predicable
{
    private List predicableList;

    /**
     * Creates an AllPredicate
     * @param predicableList A list of Predicables
     */
    public AllPredicate(List predicableList)
    {
        this.predicableList = predicableList;
    }

    /**
     * Creates an AllPredicate
     * @param predicates An array of Predicables
     */
    public AllPredicate(Predicable[] predicables)
    {
        this(Arrays.asList(predicables));
    }

    /**
     * Evaluates an object and returns True if all of the pre-supplied conditions are met
     * @param obj An object
     * @return True if all conditions are met
     */
    public boolean evaluate(Object obj)
    {
        final Iterator iter = predicableList.iterator();
        for(int i = 0; i < predicableList.size(); i++)
        {
            if(!((Predicable)iter.next()).evaluate(obj))
                return false;
        }
        
        return true;
    }
}
