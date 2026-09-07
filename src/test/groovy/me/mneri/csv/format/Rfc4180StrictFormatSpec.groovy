package me.mneri.csv.format

import me.mneri.csv.exception.UnexpectedCharacterException
import spock.lang.Specification
import spock.lang.Unroll

class Rfc4180StrictFormatSpec extends Specification {
    def provider = Rfc4180StrictFormat.provider()
    def driver = new FormatDriver(provider)

    @Unroll
    def "parses #input correctly"() {
        expect:
        driver.parse(input) == output

        where:
        input     | output
        ''        | []
        '\r'      | [['']]
        'apple\r' | [['apple']]
        '\rapple' | [[''], ['apple']]
    }

    @Unroll
    def "rejects #input with UnexpectedCharacterException"() {
        when:
        driver.parse(input)

        then:
        thrown(UnexpectedCharacterException)

        where:
        input << [
                'apple,ban"ana,cherry\r',
                'apple,"ban"ana,cherry\r',
                'apple,banana,"cherry',
                'apple,banana,"',
                'apple,""x,cherry\r',
        ]
    }
}
