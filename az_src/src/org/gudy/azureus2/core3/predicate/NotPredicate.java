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

/**
 * A NotPredicate decorates an existing Predicable to return the negative result of the evaluation
 * @version 1.0
 */
public final class NotPredicate implements Predicable
{
    private Predicable ref;

    /**
     * Creates a NotPredicate
     * @param toEvaluate A predicable to evaluate the "Not" from
     */
    public NotPredicate(Predicable toEvaluate)
    {
        this.ref = toEvaluate;
    }

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(Object obj)
    {
        return !ref.evaluate(obj);
    }
}
