package me.mneri.csv.extension

import spock.lang.Specification
import spock.lang.Unroll

class VectorHelperTest extends Specification {
    def input = new char[256]

    @Unroll
    def "bitmask(4 chars) returns matching positions for '#description'"() {
//        given:
//        matches.each { int position ->
//            input[position] = target as char
//        }
//
//        expect:
//        FormatHelper.bitmask(input, 0, 'a' as char, 'b' as char, target as char, 'd' as char) == expectedMask
//
//        where:
//        description       | target | matches            | expectedMask
//        "no matches"      | 'z'    | []                 | 0L
//        "first character" | 'z'    | [0]                | 1L
//        "last character"  | 'z'    | [63]               | Long.MIN_VALUE
//        "several matches" | 'z'    | [0, 1, 10, 31, 63] | maskOf(0, 1, 10, 31, 63)
//    }
    }
}
