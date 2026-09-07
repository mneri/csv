package me.mneri.csv.format

import me.mneri.csv.exception.UnexpectedCharacterException
import spock.lang.Specification
import spock.lang.Unroll

class Rfc4180HalfRelaxedFormatSpec extends Specification {
    def provider = Rfc4180HalfRelaxedFormat.provider()
    def driver = new FormatDriver(provider)

    @Unroll
    def "parses #input correctly"() {
        expect:
        driver.parse(input) == output

        where:
        input | output
    }

    @Unroll
    def "rejects #input with UnexpectedCharacterException"() {
        when:
        driver.parse(input)

        then:
        thrown(UnexpectedCharacterException)

        where:
        input << [
        ]
    }
}
