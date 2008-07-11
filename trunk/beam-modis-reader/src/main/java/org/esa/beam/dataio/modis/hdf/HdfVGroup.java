/*
 * $Id: HdfVGroup.java,v 1.1 2006/09/19 07:00:03 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.modis.hdf;

import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.lib.HDF;

import java.util.ArrayList;
import java.util.List;

public class HdfVGroup {

    private int _id;
    private String _name;
    private String _class;

    /**
     * Retrieves a vector of all HDF vgroups contained in the file.
     *
     * @param fileId the file identifier as returned by Hopen().
     *
     * @return a vector of groups
     */
    public static HdfVGroup[] getGroups(int fileId) throws HDFException {
        if (!HDF.getWrap().Vstart(fileId)) {
            throw new HDFException("Unable to access HDF file.");
        }

        final List<HdfVGroup> groupList = new ArrayList<HdfVGroup>();

        // get the total number of lone vgroups
        final int numGroups = HDF.getWrap().Vlone(fileId, new int[1], 0);
        if (numGroups > 0) {
            final int[] refArray = new int[numGroups];
            HDF.getWrap().Vlone(fileId, refArray, refArray.length);

            // now loop over groups
            for (int n = 0; n < refArray.length; n++) {
                final int groupId = HDF.getWrap().Vattach(fileId, refArray[n], "r");

                if (groupId == HDFConstants.FAIL) {
                    HDF.getWrap().Vdetach(groupId);
                    continue;
                }

                String[] s = {""};
                HDF.getWrap().Vgetname(groupId, s);
                final String groupName = s[0].trim();

                HDF.getWrap().Vgetclass(groupId, s);
                final String groupClass = s[0].trim();

                groupList.add(new HdfVGroup(groupId, groupName, groupClass));
            }
        }

        HDF.getWrap().Vend(fileId);

        return groupList.toArray(new HdfVGroup[groupList.size()]);
    }

    /**
     * Retrieves the identifier of the group.
     *
     * @return the groupId
     */
    public int getId() {
        return _id;
    }

    /**
     * Retrieves the name of the group
     *
     * @return the group name
     */
    public String getName() {
        return _name;
    }

    /**
     * Retrieves the hdf class of the group
     *
     * @return the class
     */
    public String getGrpClass() {
        return _class;
    }

    /**
     * Constructs a group object with given identifier, name and class
     *
     * @param groupId
     * @param groupName
     * @param groupClass
     */
    public HdfVGroup(int groupId, String groupName, String groupClass) {
        _id = groupId;
        _name = groupName;
        _class = groupClass;
    }
}
