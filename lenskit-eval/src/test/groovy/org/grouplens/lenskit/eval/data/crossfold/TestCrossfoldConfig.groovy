/*
 * LensKit, an open source recommender systems toolkit.
 * Copyright 2010-2013 Regents of the University of Minnesota and contributors
 * Work on LensKit has been funded by the National Science Foundation under
 * grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.grouplens.lenskit.eval.data.crossfold

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

import org.grouplens.lenskit.cursors.Cursors

import org.grouplens.lenskit.data.event.Rating
import org.grouplens.lenskit.eval.script.ConfigTestBase
import org.grouplens.lenskit.eval.data.traintest.TTDataSet
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import com.google.common.io.Files

/**
 * Test crossfold configuration
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class TestCrossfoldConfig extends ConfigTestBase {

    def file = File.createTempFile("tempRatings", "csv")
    def trainTestDir = Files.createTempDir()

    @Before
    void prepareFile() {
        file.deleteOnExit()
        file.append('19,242,3,881250949\n')
        file.append('296,242,3.5,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
        file.append('196,242,3,881250949\n')
    }

    @After
    void cleanUpFiles() {
        file.delete()
        trainTestDir.deleteDir()
    }

    @Test
    void testBasicCrossfold() {
        def obj = eval {
            crossfold("tempRatings") {
                source file
                partitions 10
                holdout 0.5
                order RandomOrder
                train trainTestDir.getAbsolutePath() + "/ratings.train.%d.csv"
                test trainTestDir.getAbsolutePath() + "/ratings.test.%d.csv"
            }
        }
        assertThat(obj.size(), equalTo(10))
        assertThat(obj[1], instanceOf(TTDataSet))
        def tt = obj as List<TTDataSet>
        assertThat(tt[1].name, equalTo("tempRatings.1"))
        assertThat(tt[2].attributes.get("Partition"), equalTo(2))
        assertThat(tt[3].testFactory, instanceOf(DAOFactory))
    }
    
    @Test
    void testCrossfoldByRatings() {
        def obj = eval {
            crossfold("tempRatings") {
                source file
                partitions 5
                splitUsers false
                train trainTestDir.getAbsolutePath() + "/ratings.train.%d.csv"
                test trainTestDir.getAbsolutePath() + "/ratings.test.%d.csv"
            }
        }
        assertThat(obj.size(), equalTo(5))
        assertThat(obj[1], instanceOf(TTDataSet))
        def tt = obj as List<TTDataSet>
        assertThat(tt[1].name, equalTo("tempRatings.1"))
        assertThat(tt[2].attributes.get("Partition"), equalTo(2))
        assertThat(tt[3].testFactory, instanceOf(DAOFactory))
        for (TTDataSet data: tt) {
            DAOFactory factory = data.getTestFactory();
            DataAccessObject daoSnap = factory.snapshot();
            try{
                List<Rating> ratings = Cursors.makeList(daoSnap.getEvents(Rating.class));
         
                assertThat(ratings.size(), equalTo(2))
            } finally{
                daoSnap.close();
            }
        }
    }

    @Test @Ignore("wrapper functions not supported")
    void testWrapperFunction() {
        def obj = eval {
            crossfold("tempRatings") {
                source file
                wrapper {
                    it
                }
                train "${buildDir}/temp/ratings.train.%d.csv"
                test "${buildDir}/temp/ratings.test.%d.csv"
            }
        }
        obj = obj as List<TTDataSet>
        assertThat(obj.size(), equalTo(10))
        assertThat(obj[1], instanceOf(TTDataSet))
    }
}
