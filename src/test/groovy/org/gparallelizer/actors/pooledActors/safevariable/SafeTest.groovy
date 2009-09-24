//  GParallelizer
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package org.gparallelizer.actors.pooledActors.safevariable

import java.util.concurrent.atomic.AtomicBoolean
import org.gparallelizer.actors.pooledActors.PooledActors
import org.gparallelizer.actors.pooledActors.Safe
import org.gparallelizer.dataflow.DataFlowStream
import org.gparallelizer.dataflow.DataFlowVariable
import java.util.concurrent.CyclicBarrier

public class SafeTest extends GroovyTestCase {
    public void testList() {
        def jugMembers = new Safe<List>(['Me'])  //add Me

        jugMembers.send {it.add 'James'}  //add James

        final Thread t1 = Thread.start {
            jugMembers.send {it.add 'Joe'}  //add Joe
        }

        final Thread t2 = Thread.start {
            jugMembers << {it.add 'Dave'}  //add Dave
            jugMembers << {it.add 'Alice'}  //add Alice
        }

        [t1, t2]*.join()
        assertEquals(new HashSet(['Me', 'James', 'Joe', 'Dave', 'Alice']), new HashSet(jugMembers.val))
    }

    public void testCounter() {
        final Safe counter = new Safe(0L)

        final Thread t1 = Thread.start {
            counter << {updateValue it + 1}
        }

        final Thread t2 = Thread.start {
            counter << {updateValue it + 6}
        }

        final Thread t3 = Thread.start {
            counter << {updateValue it - 2}
        }

        [t1, t2, t3]*.join()

        assert 5 == counter.val
    }

    public void testAsyncVal() {
        final Safe counter = new Safe(0L)

        final Thread t1 = Thread.start {
            counter << {updateValue it + 1}
        }

        final Thread t2 = Thread.start {
            counter << {updateValue it + 6}
        }

        final Thread t3 = Thread.start {
            counter << {updateValue it - 2}
        }

        [t1, t2, t3]*.join()

        final def result = new DataFlowVariable()
        counter.valAsync {
            result << it
        }

        assertEquals 5, result.val
    }

    public void testExplicitReply() {
        final Safe counter = new Safe(0L)

        def result = new DataFlowStream()
        PooledActors.actor {
            counter << {reply 'Explicit reply'; 10}
            react {a, b ->
                result << a
                result << b
            }
        }.start()

        assertEquals 'Explicit reply', result.val
        assertEquals 10, result.val
    }

    public void testDirectMessage() {
        final Safe counter = new Safe(0L)

        counter << null
        assertNull counter.val

        counter << 10
        assertEquals 10, counter.val

        counter << 20
        assertEquals 20, counter.val

        counter << {updateValue(it + 10)}
        assertEquals 30, counter.val

    }

    public void testDirectMessageOnNullInitialValue() {
        final Safe counter = new Safe()

        counter << 10
        assertEquals 10, counter.val
    }

    public void testNullInitialValue() {
        final Safe counter = new Safe()

        final def result = new DataFlowVariable()
        counter << {result << it}
        assertNull result.val
    }

    public void testReplies() {
        final Safe counter = new Safe(0L)

        def result = new DataFlowVariable()
        PooledActors.actor {
            counter << {it}
            react {
                result << it
            }
        }.start()
        assertEquals 0, result.val

        result = new DataFlowVariable()
        PooledActors.actor {
            counter << {null}
            react {
                result << it
            }
        }.start()
        assertNull result.val

        result = new DataFlowVariable()
        PooledActors.actor {
            counter << {}
            react {
                result << it
            }
        }.start()
        assertNull result.val

    }

    public void testAwait() {
        final Safe counter = new Safe(0L)

        final AtomicBoolean flag = new AtomicBoolean(false)
        counter << {
            updateValue it + 1
            Thread.sleep 3000
            updateValue it + 1
            flag.set true
        }
        assertFalse flag.get()
        counter.await()
        assert flag.get()
    }

    public void testInstantVal() {
        final Safe counter = new Safe(0L)

        assertEquals 0, counter.instantVal
        counter << {
            updateValue it + 1
        }
        counter.await()
        assertEquals 1, counter.instantVal
        assertEquals 1, counter.val
    }
}
