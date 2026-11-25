package day3

import day2.*
import java.util.concurrent.atomic.*

class FAABasedQueue<E> : Queue<E> {
    private val firstSegment = Segment(0)
    private val enqIdx = AtomicLong(0)
    private val deqIdx = AtomicLong(0)
    private val head: AtomicReference<Segment>
    private val tail: AtomicReference<Segment>

    init {
        head = AtomicReference(firstSegment)
        tail = AtomicReference(firstSegment)
    }

    override fun enqueue(element: E) {
        while (true) {
            val i = enqIdx.getAndIncrement()
            val segmentId = i / SEGMENT_SIZE
            val segment = findSegment(tail.get(), segmentId)
            moveTailForward(segment)
            val cellIndex = (i % SEGMENT_SIZE).toInt()
            if (segment.cells.compareAndSet(cellIndex, null, element)) return
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun dequeue(): E? {
        while (true) {
            if (deqIdx.get() >= enqIdx.get()) return null
            val i = deqIdx.getAndIncrement()
            val segmentId = i / SEGMENT_SIZE
            val segment = findSegment(head.get(), segmentId)
            moveHeadForward(segment)
            val cellIndex = (i % SEGMENT_SIZE).toInt()
            val value = segment.cells.getAndSet(cellIndex, POISONED)
            if (value == null || value === POISONED) continue
            return value as E
        }
    }

    private fun findSegment(start: Segment, targetId: Long): Segment {
        var cur = if (start.id <= targetId) start else firstSegment
        while (cur.id < targetId) {
            val next = cur.next.get() ?: Segment(cur.id + 1).also { newSegment ->
                cur.next.compareAndSet(null, newSegment)
            }
            cur = cur.next.get() ?: next
        }
        return cur
    }

    private fun moveTailForward(target: Segment) {
        while (true) {
            val curTail = tail.get()
            if (curTail.id >= target.id) return
            if (tail.compareAndSet(curTail, target)) return
        }
    }

    private fun moveHeadForward(target: Segment) {
        while (true) {
            val curHead = head.get()
            if (curHead.id >= target.id) return
            if (head.compareAndSet(curHead, target)) return
        }
    }
}

private class Segment(val id: Long) {
    val next = AtomicReference<Segment?>(null)
    val cells = AtomicReferenceArray<Any?>(SEGMENT_SIZE)
}

private val POISONED = Any()

// DO NOT CHANGE THIS CONSTANT
private const val SEGMENT_SIZE = 2
