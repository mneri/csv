package me.mneri.csv.reader

import me.mneri.csv.hint.Hint
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Specification

class ConfigurationTest extends Specification {
    def "defaults"() {
        when:
        def config = Configuration.builder().build()

        then:
        config.maxLineSize() == 4_096
        config.hints() == 0
    }

    def "the builder sets the values"() {
        when:
        def config = Configuration.builder().withMaxLineSize(100).withHints(Hint.TINY_FIELDS).build()

        then:
        config.maxLineSize() == 100
        config.hints() == Hint.TINY_FIELDS
    }

    def "rejects a max line size out of range: #size"() {
        when:
        Configuration.builder().withMaxLineSize(size)

        then:
        thrown(IllegalArgumentException)

        where:
        size << [0, -1, Configuration.MAX_LINE_SIZE_LIMIT + 1, Integer.MAX_VALUE]
    }

    def "accepts the largest max line size"() {
        expect:
        Configuration.builder().withMaxLineSize(Configuration.MAX_LINE_SIZE_LIMIT).build().maxLineSize() ==
                Configuration.MAX_LINE_SIZE_LIMIT
    }

    def "a builder can't be used after build(): #use"() {
        given:
        def builder = Configuration.builder()
        builder.build()

        when:
        action(builder)

        then:
        thrown(IllegalStateException)

        where:
        use               | action
        "withMaxLineSize" | { Configuration.Builder b -> b.withMaxLineSize(100) }
        "withHints"       | { Configuration.Builder b -> b.withHints(Hint.TINY_FIELDS) }
        "build"           | { Configuration.Builder b -> b.build() }
    }
}
