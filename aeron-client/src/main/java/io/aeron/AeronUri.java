/*
 * Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron;

import java.util.*;

/**
 * Parser for Aeron channel URIs. The format is:
 * <pre>
 * aeron-uri = "aeron:" media [ "?" param *( "|" param ) ]
 * media     = *( "[^?:]" )
 * param     = key "=" value
 * key       = *( "[^=]" )
 * value     = *( "[^|]" )
 * </pre>
 * <p>
 * Multiple params with the same key are allowed, the last value specified takes precedence.
 * @see ChannelUriBuilder
 */
public class AeronUri
{
    private enum State
    {
        MEDIA, PARAMS_KEY, PARAMS_VALUE
    }

    /**
     * URI Scheme for Aeron channels.
     */
    public static final String AERON_SCHEME = "aeron";

    private static final String AERON_PREFIX = AERON_SCHEME + ":";

    private final String media;
    private final Map<String, String> params;

    /**
     * Construct with the components provided to avoid parsing.
     *
     * @param media  for the channel which is typically "udp" or "ipc".
     * @param params for the query string as key value pairs.
     */
    public AeronUri(final String media, final Map<String, String> params)
    {
        this.media = media;
        this.params = params;
    }

    /**
     * The media over which the channel operates.
     *
     * @return the media over which the channel operates.
     */
    public String media()
    {
        return media;
    }

    /**
     * The scheme for the URI. Must be "aeron".
     *
     * @return the scheme for the URI.
     */
    public String scheme()
    {
        return AERON_SCHEME;
    }

    /**
     * Get a value for a given parameter key.
     *
     * @param key to lookup.
     * @return the value if set for the key otherwise null.
     */
    public String get(final String key)
    {
        return params.get(key);
    }

    /**
     * Get the value for a given parameter key or the default value provided if the key does not exist.
     *
     * @param key          to lookup.
     * @param defaultValue to be returned if no key match is found.
     * @return the value if set for the key otherwise the default value provided.
     */
    public String get(final String key, final String defaultValue)
    {
        final String value = params.get(key);
        if (null != value)
        {
            return value;
        }

        return defaultValue;
    }

    /**
     * Does the URI contain a value for the given key.
     *
     * @param key to be lookup.
     * @return true if the key has a value otherwise false.
     */
    public boolean containsKey(final String key)
    {
        return params.containsKey(key);
    }

    /**
     * Generate a String representation of the URI that is valid for an Aeron channel.
     *
     * @return a String representation of the URI that is valid for an Aeron channel.
     */
    public String toString()
    {
        final StringBuilder sb = new StringBuilder((params.size() * 20) + 10)
            .append(AERON_PREFIX)
            .append(media);

        if (params.size() > 0)
        {
            sb.append('?');

            for (final Map.Entry<String, String> entry : params.entrySet())
            {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append('|');
            }

            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    /**
     * Parse a {@link CharSequence} which contains an Aeron URI.
     *
     * @param cs to be parsed.
     * @return a new {@link AeronUri} representing the URI string.
     */
    public static AeronUri parse(final CharSequence cs)
    {
        if (!startsWith(cs, AERON_PREFIX))
        {
            throw new IllegalArgumentException("Aeron URIs must start with 'aeron:', found: '" + cs + "'");
        }

        final StringBuilder builder = new StringBuilder();
        final Map<String, String> params = new HashMap<>();
        String media = null;
        String key = null;

        State state = State.MEDIA;
        for (int i = AERON_PREFIX.length(); i < cs.length(); i++)
        {
            final char c = cs.charAt(i);

            switch (state)
            {
                case MEDIA:
                    switch (c)
                    {
                        case '?':
                            media = builder.toString();
                            builder.setLength(0);
                            state = State.PARAMS_KEY;
                            break;

                        case ':':
                            throw new IllegalArgumentException("Encountered ':' within media definition");

                        default:
                            builder.append(c);
                    }
                    break;

                case PARAMS_KEY:
                    switch (c)
                    {
                        case '=':
                            key = builder.toString();
                            builder.setLength(0);
                            state = State.PARAMS_VALUE;
                            break;

                        default:
                            builder.append(c);
                    }
                    break;

                case PARAMS_VALUE:
                    switch (c)
                    {
                        case '|':
                            params.put(key, builder.toString());
                            builder.setLength(0);
                            state = State.PARAMS_KEY;
                            break;

                        default:
                            builder.append(c);
                    }
                    break;

                default:
                    throw new IllegalStateException("Que? state=" + state);
            }
        }

        switch (state)
        {
            case MEDIA:
                media = builder.toString();
                break;

            case PARAMS_VALUE:
                params.put(key, builder.toString());
                break;

            default:
                throw new IllegalArgumentException("No more input found, but was in state: " + state);
        }

        return new AeronUri(media, params);
    }

    private static boolean startsWith(final CharSequence input, final CharSequence prefix)
    {
        if (input.length() < prefix.length())
        {
            return false;
        }

        for (int i = 0; i < prefix.length(); i++)
        {
            if (input.charAt(i) != prefix.charAt(i))
            {
                return false;
            }
        }

        return true;
    }
}
