package org.gparallelizer.actors.pooledActors

import org.gparallelizer.actors.pooledActors.PooledActors

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Feb 20, 2009
 */

public class SingleThreadedPooledMergeSortTest extends PooledMergeSortTest {

    protected void setUp() {
        super.setUp();
        PooledActors.pool.initialize(1)
    }
}