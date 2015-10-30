/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.util.Pair;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Queue of things to run on a looper thread.  Items posted with {@link #post} will not
 * be actually enqued on the handler until after the last one has run, to keep from
 * starving the thread.
 *
 * DeferredHandler本身当中的mQueue变量会将所有的对DeferredHandler的post请求调用的Runnable都保存到
 * mQueue当中(这个mQueue变量并不是真正的MessageQueue，而只是一个简单的LinkedList)。但是DeferredHandler
 * 并不会一次性将mQueue当中所有的Runnable都放入到MessageQueue当中，一次只放一个，只有当放入的这个的Runnable
 * 被执行了(不保证执行结束啊)，然后再放入另一个Runnable到MessageQueue当中。
 *
 *
 *
 * <p/>
 * This class is fifo.
 */
public class DeferredHandler {

    private static final String TAG = "DeferredHandler";

    private LinkedList<Pair<Runnable, Integer>> mQueue = new LinkedList<Pair<Runnable, Integer>>();
    private MessageQueue mMessageQueue = Looper.myQueue();
    private Impl mHandler = new Impl();

    // TODO: 参考<Efficient Android Threading>来对MessageQueue当中的IdleHandler的作用
    /**
     * MessageQueue.IdleHandler本身是一个callback interface for discovering when a
     * thread is going to block waiting for more messages.
     *
     *
     */
    private class Impl extends Handler implements MessageQueue.IdleHandler {
        public void handleMessage(Message msg) {
            Log.d(TAG, " inside the Handler Instance, and we have received the empty message ... ");
            Pair<Runnable, Integer> p;
            Runnable r;
            synchronized (mQueue) {
                if (mQueue.size() == 0) {
                    Log.d(TAG, " do not have task to run for now...");
                    return;
                }
                // p中保存的就是the removed object.
                p = mQueue.removeFirst();
                r = p.first;
            }
            Log.d(TAG, " and finally, run this task, and finished the current work ... ");
            r.run();
            synchronized (mQueue) {
                scheduleNextLocked();
            }
        }

        /**
         * Called when the message queue has run out of messages and will now
         * wait for more.  Return true to keep your idle handler active, false
         * to have it removed.  This may be called if there are still messages
         * pending in the queue, but they are all scheduled to be dispatched
         * after the current time.
         */
        public boolean queueIdle() {
            // 当我们从调用DeferredHandler时，如果我们传递的不是普通的Runnable，而是一个IdleRunnable
            // 那么queueIdle()方法就会被调用执行
            Log.d(TAG, " -------------- QUEUE IDLE --------------");
            handleMessage(null);
            return false;
        }
    }

    private class IdleRunnable implements Runnable {
        Runnable mRunnable;

        IdleRunnable(Runnable r) {
            mRunnable = r;
        }

        public void run() {
            mRunnable.run();
        }
    }

    public DeferredHandler() {
    }

    /**
     * Schedule runnable to run after everything that's on the queue right now.
     */
    public void post(Runnable runnable) {
        post(runnable, 0);
    }

    public void post(Runnable runnable, int type) {
        synchronized (mQueue) {
            Log.d(TAG, " first, add this runnable into the mQueue we kept...");
            mQueue.add(new Pair<Runnable, Integer>(runnable, type));
            if (mQueue.size() == 1) {
                // 即如果mQueue的大小是大于一的或者mQueue当中没有内容，那么我们就不处理
                // 因为我们需要保证mQueue当中永远只有一个Runnable
                Log.d(TAG, " we have received the Runnable events, and scheduleNextLocked ... ");
                scheduleNextLocked();
            }
        }
    }

    /**
     * Schedule runnable to run when the queue goes idle.
     */
    public void postIdle(final Runnable runnable) {
        postIdle(runnable, 0);
    }

    public void postIdle(final Runnable runnable, int type) {
        post(new IdleRunnable(runnable), type);
    }

    public void cancelRunnable(Runnable runnable) {
        synchronized (mQueue) {
            while (mQueue.remove(runnable)) {
            }
        }
    }

    public void cancelAllRunnablesOfType(int type) {
        synchronized (mQueue) {
            ListIterator<Pair<Runnable, Integer>> iter = mQueue.listIterator();
            Pair<Runnable, Integer> p;
            while (iter.hasNext()) {
                p = iter.next();
                if (p.second == type) {
                    iter.remove();
                }
            }
        }
    }

    public void cancel() {
        synchronized (mQueue) {
            mQueue.clear();
        }
    }

    /**
     * Runs all queued Runnables from the calling thread.
     */
    public void flush() {
        LinkedList<Pair<Runnable, Integer>> queue = new LinkedList<Pair<Runnable, Integer>>();
        synchronized (mQueue) {
            queue.addAll(mQueue);
            mQueue.clear();
        }
        for (Pair<Runnable, Integer> p : queue) {
            p.first.run();
        }
    }

    void scheduleNextLocked() {
        if (mQueue.size() > 0) {
            Pair<Runnable, Integer> p = mQueue.getFirst();
            Runnable peek = p.first;
            if (peek instanceof IdleRunnable) {
                Log.d(TAG, " the current runnable are the IdleRunnable, and we add it into the MessageQueue");
                mMessageQueue.addIdleHandler(mHandler);
            } else {
                Log.d(TAG, " the current runnable are not IdleRunnable, just using mHandler to handle it ... ");
                mHandler.sendEmptyMessage(1);
            }
        }
    }
}

