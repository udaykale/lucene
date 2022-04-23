extern crate libc;

use libc::{c_int};

// This is the interface to the JVM that we'll call the majority of our methods on.
use jni::JNIEnv;

// These objects are what you should use as arguments to your native
// function. They carry extra lifetime information to prevent them escaping
// this context and getting used after being GC'd.
use jni::objects::JObject;

#[no_mangle]
pub extern fn Java_org_apache_lucene_index_memory_MemoryIndex_add(env: JNIEnv, class: JObject,
                                                                  v1: c_int, v2: c_int) -> c_int {
    v1 + v2
}