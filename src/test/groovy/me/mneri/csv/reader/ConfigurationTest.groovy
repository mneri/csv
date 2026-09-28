package me.mneri.csv.reader

import me.mneri.csv.concurrent.DefaultThreadFactory
import me.mneri.csv.deserializer.StringListDeserializer
import me.mneri.csv.format.Rfc4180FullyRelaxedFormat
import me.mneri.csv.hint.Hint
import me.mneri.csv.reader.CsvReader.Configuration
import spock.lang.Specification

import java.nio.BufferOverflowException

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

    def "rejects a non-positive max line size: #size"() {
        when:
        Configuration.builder().withMaxLineSize(size)

        then:
        thrown(IllegalArgumentException)

        where:
        size << [0, -1]
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

    def "#mode #hints accepts a line of #length characters: #accepted"() {
        given:
        def config = Configuration.builder().withMaxLineSize(100).withHints(hints).build()
        def line = "a" * (length - 1) + "\n"
        def input = "b\n" + line + "c\n"
        def rdr = new StringReader(input)
        def reader = mode == "sequential"
                ? CsvReader.open(rdr, Rfc4180FullyRelaxedFormat.provider(), new StringListDeserializer(), config)
                : CsvReader.parallel(new DefaultThreadFactory(), rdr, Rfc4180FullyRelaxedFormat.provider(),
                        new StringListDeserializer(), config)
        def lines = []
        def overflow = false

        when:
        try {
            while (reader.hasNext()) {
                lines << reader.next()
            }
        } catch (BufferOverflowException ignored) {
            overflow = true
        } finally {
            reader.close()
        }

        then:
        overflow == !accepted
        lines == (accepted ? [["b"], ["a" * (length - 1)], ["c"]] : [["b"]])

        where:
        [mode, hints, length] << [["sequential", "parallel"], [0, Hint.TINY_FIELDS], [99, 100, 101]].combinations()
        accepted = length <= 100
    }
}
