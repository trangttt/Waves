enablePlugins(JmhPlugin)

addCommandAlias("lang-run-performance-test",
                "; lang/clean; lang/jmh:run -i 5 -wi 5 -f1 -t1 com.wavesplatform.lang.benchmark.GlobalFunctionsBenchmark")
