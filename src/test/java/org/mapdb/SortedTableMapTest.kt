package org.mapdb

import org.junit.Test
import org.junit.Assert.*
import java.util.*
import kotlin.test.assertFailsWith

class SortedTableMapTest{

    @Test fun import0(){
        test(0)
    }
    @Test fun import6(){
        test(6)
    }

    @Test fun import40(){
        test(40)
    }


    @Test fun import100(){
        test(100)
    }

    @Test fun import1000(){
        test(1000)
    }

    @Test fun importMega(){
        test(1000000)
    }


    @Test fun header(){
        val volume = CC.DEFAULT_MEMORY_VOLUME_FACTORY.makeVolume(null, false)
        val consumer = SortedTableMap.import(
                keySerializer = Serializer.INTEGER,
                valueSerializer = Serializer.INTEGER,
                volume = volume
        )
        consumer.take(1,1)
        val map = consumer.finish()
        assertEquals(CC.FILE_HEADER, volume.getUnsignedByte(0).toLong())
        assertEquals(CC.FILE_TYPE_SORTED_SINGLE, volume.getUnsignedByte(1).toLong())
    }

    fun test(size:Int){
        val consumer = SortedTableMap.import(
                keySerializer = Serializer.INTEGER,
                valueSerializer = Serializer.INTEGER,
                volume = CC.DEFAULT_MEMORY_VOLUME_FACTORY.makeVolume(null, false)
        )
        for(i in 0 until size*3 step 3){
            consumer.take(Pair(i, i*2))
        }

        val map = consumer.finish()

        if(size!=0 && size<10000)
            assertArrayEquals(arrayOf(0), map.keySerializer.valueArrayToArray(map.pageKeys))
        assertEquals(size, map.size)

        var keyIter = map.keyIterator()
        var valueIter = map.valueIterator()
        var entryIter = map.entryIterator()

        for(i in 0 until size*3 step 3) {
            assertEquals(i*2, map[i])

            assertTrue(keyIter.hasNext())
            assertEquals(i, keyIter.next())

            assertTrue(valueIter.hasNext())
            assertEquals(i*2, valueIter.next())

            assertTrue(entryIter.hasNext())
            val node = entryIter.next()
            assertEquals(i, node.key)
            assertEquals(i*2, node.value)
        }
        assertFalse(keyIter.hasNext())
        assertFailsWith(NoSuchElementException::class){
            keyIter.next()
        }
        assertFalse(valueIter.hasNext())
        assertFailsWith(NoSuchElementException::class){
            valueIter.next()
        }
        assertFalse(entryIter.hasNext())
        assertFailsWith(NoSuchElementException::class){
            entryIter.next()
        }


        //test lower, higher etc
        val notEmpty = map.isEmpty().not()
        for(i in -2 until size*3+2){
            val notin = i%3!=0 || i<0 || i>=size*3
            val expected = if(notin) null else  i*2
            assertEquals(expected, map[i] )
            val maxKey = size*3-3
            assertEquals(if(i>0 && notEmpty) Math.min(maxKey,((i-1)/3)*3) else null , map.lowerKey(i))
            assertEquals(if(i>=0 && notEmpty) Math.min(maxKey,(i/3)*3) else null , map.floorKey(i))
            assertEquals(if(i<maxKey && notEmpty) Math.max(0, DataIO.roundUp(i+1,3)) else null , map.higherKey(i))
            assertEquals(if(i<=maxKey && notEmpty) Math.max(0, DataIO.roundUp(i,3)) else null , map.ceilingKey(i))
        }

        //do reverse iterators
        keyIter = map.descendingKeyIterator()
        valueIter = map.descendingValueIterator()
        entryIter = map.descendingEntryIterator()

        for(i in size*3-3 downTo 0 step 3) {
            assertEquals(i*2, map[i])

            assertTrue(keyIter.hasNext())
            assertEquals(i, keyIter.next())

            assertTrue(valueIter.hasNext())
            assertEquals(i*2, valueIter.next())

            assertTrue(entryIter.hasNext())
            val node = entryIter.next()
            assertEquals(i, node.key)
            assertEquals(i*2, node.value)
        }
        assertFalse(keyIter.hasNext())
        assertFailsWith(NoSuchElementException::class){
            keyIter.next()
        }
        assertFalse(valueIter.hasNext())
        assertFailsWith(NoSuchElementException::class){
            valueIter.next()
        }
        assertFalse(entryIter.hasNext())
        assertFailsWith(NoSuchElementException::class){
            entryIter.next()
        }

    }


    @Test fun entry_iterator_values_issue685(){
        val consumer = SortedTableMap.import(
                keySerializer = Serializer.INTEGER,
                valueSerializer = Serializer.INTEGER,
                volume = CC.DEFAULT_MEMORY_VOLUME_FACTORY.makeVolume(null, false)
        )
        val size = 1e6.toInt()
        for(i in 0 until size){
            consumer.take(Pair(i, i*2))
        }

        val map = consumer.finish()

        val iter = map.iterator()
        var count = 0;
        while(iter.hasNext()){
            val next = iter.next()
            assertEquals(count, next.key)
            assertEquals(count*2, next.value)
            count++
        }

        val iter3 = map.descendingMap().iterator()
        while(iter3.hasNext()){
            count--
            val next = iter3.next()
            assertEquals(count, next.key)
            assertEquals(count*2, next.value)
        }


    }

}