// "Split into filter's chain" "true"

import java.util.stream.Stream;
class Test {
  void foo(Stream<String> stringStream ) {
    stringStream.filter(name -> name.startsWith("A") //starts with A 
                                &<caret>&  /*comment*/name.length() > 1).findAny();
  }
}
