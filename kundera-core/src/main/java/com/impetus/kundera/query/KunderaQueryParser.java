/*******************************************************************************
 * * Copyright 2012 Impetus Infotech.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 ******************************************************************************/
package com.impetus.kundera.query;

import java.util.StringTokenizer;

/**
 * Parser for handling JPQL Single-String queries. Takes a JPQLQuery and the
 * query string and parses it into its constituent parts, updating the JPQLQuery
 * accordingly with the result that after calling the parse() method the
 * JPQLQuery is populated.
 * 
 * <pre>
 * SELECT [ {result} ]
 * [FROM {candidate-classes} ]
 * [WHERE {filter}]
 * [GROUP BY {grouping-clause} ]
 * [HAVING {having-clause} ]
 * [ORDER BY {ordering-clause}]
 * e.g SELECT c FROM Customer c INNER JOIN c.orders o WHERE c.status = 1
 * </pre>
 * 
 * @author animesh.kumar
 */
public class KunderaQueryParser
{

    /** The JPQL query to populate. */
    private KunderaQuery query;

    /** The single-string query string. */
    private String queryString;

    /**
     * Record of the keyword currently being processed, so we can check for out
     * of order keywords.
     */
    // private int keywordPosition = -1;

    /**
     * Constructor for the Single-String parser.
     * 
     * @param query
     *            The query
     * @param queryString
     *            The Single-String query
     */
    public KunderaQueryParser(KunderaQuery query, String queryString)
    {
        this.query = query;
        this.queryString = queryString;
    }

    /**
     * Method to parse the Single-String query.
     */
    public final void parse()
    {
        new Compiler(new Parser(queryString)).compile();
    }

    /**
     * Method to detect whether this token is a keyword for JPQL Single-String.
     * 
     * @param token
     *            The token
     * 
     * @return Whether it is a keyword
     */
    private boolean isKeyword(String token)
    {
        // Compare the passed token against the provided keyword list, or their
        // lowercase form
        for (int i = 0; i < KunderaQuery.SINGLE_STRING_KEYWORDS.length; i++)
        {
            if (token.equalsIgnoreCase(KunderaQuery.SINGLE_STRING_KEYWORDS[i]))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Compiler to process keywords contents. In the query the keywords often
     * have content values following them that represent the constituent parts
     * of the query. This takes the keyword and sets the constituent part
     * accordingly.
     */
    private class Compiler
    {

        /** The tokenizer. */
        private Parser tokenizer;

        // Temporary variable since grouping clause is made up of GROUP BY ...
        // HAVING ...
        /** The grouping clause. */
        private String groupingClause;

        /**
         * Instantiates a new compiler.
         * 
         * @param tokenizer
         *            the tokenizer
         */
        Compiler(Parser tokenizer)
        {
            this.tokenizer = tokenizer;
        }

        /**
         * Compile.
         */
        private void compile()
        {
            // if it is not an update statement
            if (!compileUpdate())
            {
                compileSelectOrDelete();
            }
            // any keyword after compiling the SELECT is an error
            String keyword = tokenizer.parseKeyword();
            if (keyword != null)
            {
                if (isKeyword(keyword))
                {
                    throw new JPQLParseException("out of order keyword: " + keyword);
                }
            }
        }

        /**
         * Compile update.
         * 
         * @return true, if successful
         */
        private boolean compileUpdate()
        {
            if (tokenizer.parseKeywordIgnoreCase("UPDATE"))
            {
                query.setIsDeleteUpdate(true);
                compileFrom();

                if (tokenizer.parseKeywordIgnoreCase("SET"))
                {
                    compileUpdateClause();
                }

                compilewhereClause();

                return true;
            }
            return false;
        }

        /**
         * Compile select.
         */
        private void compileSelectOrDelete()
        {
            if (!(tokenizer.parseKeywordIgnoreCase("SELECT")))
            {
                if (tokenizer.parseKeywordIgnoreCase("DELETE"))
                {
                    query.setIsDeleteUpdate(true);

                }
            }

            compileResult();
            if (tokenizer.parseKeywordIgnoreCase("FROM"))
            {
                compileFrom();
            }
            compilewhereClause();
        }

        /**
         * Compilewhere clause.
         */
        private void compilewhereClause()
        {
            if (tokenizer.parseKeywordIgnoreCase("WHERE"))
            {
                compileWhere();
            }
            if (tokenizer.parseKeywordIgnoreCase("GROUP BY"))
            {
                compileGroup();
            }
            if (tokenizer.parseKeywordIgnoreCase("HAVING"))
            {
                compileHaving();
            }
            if (groupingClause != null)
            {
                query.setGrouping(groupingClause);
            }

            if (tokenizer.parseKeywordIgnoreCase("ORDER BY"))
            {
                compileOrder();
            }
        }

        /**
         * Compile result.
         */
        private void compileResult()
        {
            String content = tokenizer.parseContent();
            String[] result = null;
            int count = 0;
            // content may be empty
            if (content.length() > 0)
            {
                StringTokenizer stringTokenizer = new StringTokenizer(content, ",");
                result = new String[stringTokenizer.countTokens() + 1];
                while (stringTokenizer.hasMoreTokens())
                {
                    String property = stringTokenizer.nextToken();
                    if (property.indexOf(".") > 0)
                    {
                        result[0] = property.substring(0, property.indexOf("."));
                        String fieldName = property.substring(property.indexOf(".") + 1, property.length());
                        if (fieldName == null || fieldName.isEmpty())
                        {
                            throw new JPQLParseException(
                                    "You have not given any column name after . ,Column name should not be empty");
                        }
                        if (result[count] == null)
                        {
                            throw new JPQLParseException("Bad query format");
                        }
                        result[++count] = fieldName;
                    }
                    else
                    {
                        if (count > 0)
                        {
                            throw new JPQLParseException("Bad query format");
                        }
                        result[count] = content;
                        count++;
                    }
                }
                query.setResult(result);
            }
        }

        /**
         * Compile from.
         */
        private void compileFrom()
        {
            String content = tokenizer.parseContent();
            // content may be empty
            if (content.length() > 0)
            {
                query.setFrom(content);
            }
        }

        /**
         * Compile from.
         */
        private void compileUpdateClause()
        {
            String content = tokenizer.parseContent();
            // content may be empty
            if (content.length() > 0)
            {
                String[] colArr = tokenizeColumn(content);
                addUpdateClause(colArr);
            }
        }

        /**
         * Tokenize column along with it's value using "," as tokenizer
         * 
         * @param content
         *            content
         * @return array of tokenized tuple.
         */
        private String[] tokenizeColumn(String content)
        {
            StringTokenizer tokenizer = new StringTokenizer(content, ",");
            String[] columnArr = tokenizer.countTokens() > 0 ? new String[tokenizer.countTokens()] : null;
            int count = 0;
            while (tokenizer.hasMoreTokens())
            {
                columnArr[count++] = tokenizer.nextToken();
            }

            return columnArr;
        }

        private void addUpdateClause(final String[] clauseArr)
        {
            for (String columnTuple : clauseArr)
            {
                StringTokenizer tokenizer = new StringTokenizer(columnTuple, ".");
                String value = getTokenizedValue(tokenizer);
                StringTokenizer token = new StringTokenizer(value, "=");
                while (token.hasMoreTokens())
                {
                    query.addUpdateClause(token.nextToken(), token.nextToken().trim());
                }
            }
        }

        private String getTokenizedValue(StringTokenizer tokenizer)
        {
            String value = null;

            while (tokenizer.hasMoreTokens())
            {
                value = tokenizer.nextToken();
            }
            return value;
        }

        /**
         * Compile where.
         */
        private void compileWhere()
        {
            String content = tokenizer.parseContent();
            // content cannot be empty
            if (content.length() == 0)
            {
                throw new JPQLParseException("keyword without value[WHERE]");
            }
            query.setFilter(content);
        }

        /**
         * Compile group.
         */
        private void compileGroup()
        {
            String content = tokenizer.parseContent();
            // content cannot be empty
            if (content.length() == 0)
            {
                throw new JPQLParseException("keyword without value: GROUP BY");
            }
            groupingClause = content;
        }

        /**
         * Compile having.
         */
        private void compileHaving()
        {
            String content = tokenizer.parseContent();
            // content cannot be empty
            if (content.length() == 0)
            {
                throw new JPQLParseException("keyword without value: HAVING");
            }
            if (groupingClause != null)
            {
                groupingClause = groupingClause.trim() + content;
            }
            else
            {
                groupingClause = content;
            }
        }

        /**
         * Compile order.
         */
        private void compileOrder()
        {
            String content = tokenizer.parseContent();
            // content cannot be empty
            if (content.length() == 0)
            {
                throw new JPQLParseException("keyword without value: ORDER BY");
            }
            query.setOrdering(content);
        }
    }

    /**
     * Tokenizer that provides access to current token.
     */
    private class Parser
    {

        /** tokens. */
        private final String[] tokens;

        /** keywords. */
        private final String[] keywords;

        /** current token cursor position. */
        private int pos = -1;

        /**
         * Constructor.
         * 
         * @param str
         *            the str
         */
        public Parser(String str)
        {
            StringTokenizer tokenizer = new StringTokenizer(str);
            tokens = new String[tokenizer.countTokens()];
            keywords = new String[tokenizer.countTokens()];
            int i = 0;
            while (tokenizer.hasMoreTokens())
            {
                tokens[i++] = tokenizer.nextToken();
            }
            for (i = 0; i < tokens.length; i++)
            {
                if (isKeyword(tokens[i]))
                {
                    keywords[i] = tokens[i];
                }
                else if (i < tokens.length - 1 && isKeyword(tokens[i] + ' ' + tokens[i + 1]))
                {
                    keywords[i] = tokens[i];
                    i++;
                    keywords[i] = tokens[i];
                }
            }
        }

        /**
         * Parse the content until a keyword is found.
         * 
         * @return the content
         */
        public String parseContent()
        {
            String content = "";
            while (pos < tokens.length - 1)
            {
                pos++;
                if (isKeyword(tokens[pos]))
                {
                    pos--;
                    break;
                }
                else if (pos < tokens.length - 1 && isKeyword(tokens[pos] + ' ' + tokens[pos + 1]))
                {
                    pos--;
                    break;
                }
                else
                {
                    if (content.length() == 0)
                    {
                        content = tokens[pos];
                    }
                    else
                    {
                        content += " " + tokens[pos];
                    }
                }
            }
            return content;
        }

        /**
         * Parse the next token looking for a keyword. The cursor position is
         * skipped in one tick if a keyword is found
         * 
         * @param keyword
         *            the searched keyword
         * 
         * @return true if the keyword
         */
        public boolean parseKeyword(String keyword)
        {
            if (pos < tokens.length - 1)
            {
                pos++;
                if (keywords[pos] != null)
                {
                    if (keywords[pos].equals(keyword))
                    {
                        return true;
                    }
                    if (keyword.indexOf(' ') > -1)
                    {
                        if (pos < keywords.length - 1)
                        {
                            if ((keywords[pos] + ' ' + keywords[pos + 1]).equals(keyword))
                            {
                                pos++;
                                return true;
                            }
                        }
                    }
                }
                pos--;
            }
            return false;
        }

        /**
         * Parse the next token looking for a keyword. The cursor position is
         * skipped in one tick if a keyword is found
         * 
         * @param keyword
         *            the searched keyword
         * 
         * @return true if the keyword
         */
        public boolean parseKeywordIgnoreCase(String keyword)
        {
            if (pos < tokens.length - 1)
            {
                pos++;
                if (keywords[pos] != null)
                {
                    if (keywords[pos].equalsIgnoreCase(keyword))
                    {
                        return true;
                    }
                    if (keyword.indexOf(' ') > -1)
                    {
                        if ((keywords[pos] + ' ' + keywords[pos + 1]).equalsIgnoreCase(keyword))
                        {
                            pos++;
                            return true;
                        }
                    }
                }
                pos--;
            }
            return false;
        }

        /**
         * Parse the next token looking for a keyword. The cursor position is
         * skipped in one tick if a keyword is found
         * 
         * @return the parsed keyword or null
         */
        public String parseKeyword()
        {
            if (pos < tokens.length - 1)
            {
                pos++;
                if (keywords[pos] != null)
                {
                    return keywords[pos];
                }
                pos--;
            }
            return null;
        }
    }

}
