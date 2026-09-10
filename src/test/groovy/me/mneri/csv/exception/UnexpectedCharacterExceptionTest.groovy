package me.mneri.csv.exception

import spock.lang.Specification

class UnexpectedCharacterExceptionTest extends Specification {
    def "getPosition() returns the correct value"() {
        given:
        def position = 5L

        when:
        def e = new UnexpectedCharacterException(position)

        then:
        e.getPosition() == position
    }
}
