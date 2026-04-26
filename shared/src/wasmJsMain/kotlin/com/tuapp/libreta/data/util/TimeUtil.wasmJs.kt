package com.tuapp.libreta.data.util

@JsFun("""function() { 
  return typeof Date !== 'undefined' ? Date.now() : 0; 
}""")
external fun dateNow(): Double

@JsFun("""function() { 
  return typeof performance !== 'undefined' && performance.now 
    ? performance.now() 
    : (typeof Date !== 'undefined' ? Date.now() : 0); 
}""")
external fun performanceNow(): Double

actual fun currentEpochMs(): Long = dateNow().toLong()
actual fun monotonicTimeMs(): Long = performanceNow().toLong()