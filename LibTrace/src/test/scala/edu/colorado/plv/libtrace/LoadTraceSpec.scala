package edu.colorado.plv.libtrace

import org.scalatest.FlatSpec
import java.io.File

/**
  * @author Shawn Meier
  *         Created on 9/8/17.
  */
class LoadTraceSpec extends FlatSpec {
  "LoadTrace" should "Load a trace without crashing" in {
    val trace = LoadTrace.loadTrace(new File("/Users/s/Documents/data/monkey_traces/output-synt/yxl-DownloadProvider-bin/monkeyTraces/trace_2017-04-11_04:25:53.repaired"))
  }
}
