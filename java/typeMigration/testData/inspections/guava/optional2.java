import com.google.common.base.Optional;

class A {

  Optional<String> m1() {
    Optiona<caret>l<String> o = Optional.absent();
    String s = o.get();
    return o;
  }

  void w1() {
    Optional<String> someVar = m1();
    System.out.println(someVar.get());
    System.out.println(someVar.isPresent() ? Collections.singleton(someVar.get()) : Collections.emptySet());
  }

}