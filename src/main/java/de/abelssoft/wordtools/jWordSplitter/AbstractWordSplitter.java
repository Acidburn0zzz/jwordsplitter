/**
 * Copyright 2004-2007 Sven Abels
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.abelssoft.wordtools.jWordSplitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

/**
 * This class can split words into their smallest parts (atoms). For example "Erhebungsfehler"
 * will be splitted into "erhebung" and "fehler".
 * 
 * Please note: We don't expect to have any special chars here (!":;,.-_, etc.). Only a set of 
 * characters and only one word.
 * 
 * This method is especially beneficial for German words but it will work with all languages.
 * The order of the words in the collection will be identical to their appeareance in the 
 * connected word. It's good to provide a large dictionary.
 * 
 * @author Sven Abels (Abelssoft), Sven@abelssoft.de
 * @author Daniel Naber
 * @version 2.0
 */
public abstract class AbstractWordSplitter
{
    
    private Set<String> words = null;
    private boolean hideConnectingCharacters = true;
    private boolean strictMode = false;
    protected String plainTextDictFile = null;

    protected abstract Set<String> getWordList() throws IOException;
    protected abstract int getMinimumWordLength();
    protected abstract Collection<String> getConnectingCharacters();
    
    /**
     * @param hideConnectingCharacters shall the splitted wordset still contain
     *  the connecting character? (default=true) 
     * @throws IOException 
     */
    public AbstractWordSplitter(boolean hideConnectingCharacters) throws IOException
    {
      this(hideConnectingCharacters, null);
    }

    public AbstractWordSplitter(boolean hideConnectingCharacters, String plainTextDictFile) throws IOException 
    {
      this.hideConnectingCharacters = hideConnectingCharacters;
      this.plainTextDictFile = plainTextDictFile;
      words = getWordList();
    }
    
    /**
     * @throws IOException 
     */
    public AbstractWordSplitter() throws IOException
    {
      this(true);
    }    

    /**
     * When set to true, words will only be splitted if all parts are words.
     * Otherwise the splitting result might contain parts that are not words.
     * Only if this is set to true, the minimum length of word parts is correctly
     * taken into account.
     */
    public void setStrictMode(boolean strictMode) {
      this.strictMode = strictMode;
    }
    
    /**
     * Detect if a word exists in the dictionary. Words that are too short are ignored 
     * in order to avoid a fragmentation, which is too strong.
     */
    private boolean isWord(String s)
    {
        if (s==null)
          return false;
        if (s.trim().length()<getMinimumWordLength())
          return false;
        if (words.contains(s.toLowerCase().trim()))
          return true;
        return false;
    }
    

    /**
     * Split a compound word into its parts. If the word cannot be split (e.g.
     * because it's unknown or it is not a compound), one part with the
     * word itself is returned.
     * 
     * <p>Attention: We don't expect to have any special chars here (!":;,.-_, etc.).
     * 
     * @param str a single compound word
     */
    public Collection<String> splitWord(String str)
    {
        Collection<String> result=new ArrayList<String>();
        if (str==null)
          return result;
        String s=str.trim();
        if (s.length()<2)
        {
            result.add(s);
            return result;
        }
        
        //find a tupel (from left to right):
        Collection<String> tupel=findTupel(s);
        if (tupel==null && !strictMode)
          tupel=truncateSplit(s);
        if (tupel==null && !strictMode)
          tupel=truncateSplitReverse(s);
        if (tupel==null)
          result.add(str);
       	else
     	    result.addAll(tupel);
        
        return result;
    }
    
    
    /**
     * We were not able to split the word...well: Let's try to cut it at its beginning.
     */
    private Collection<String> truncateSplit(String s)
    {
        //we were not able to split the word...well: Let's try to cut it:
        for (int i=0;i<(s.length()-2);i++)
        {
            Collection<String> tmp=findTupel(s.substring(i));
            if (tmp!=null)
            {
                Collection<String> tmp2=new ArrayList<String>();
                if (strictMode && !isWord(s.substring(0,i))) {
                  continue;
                }
                tmp2.add(s.substring(0,i));
                tmp2.addAll(tmp);
                return tmp2;
            }
        }
        return null;
    }
    

    /**
     * We were not able to split the word... well: Let's try to cut it at its end.
     */
    private Collection<String> truncateSplitReverse(String s)
    {
        //we were not able to split the word...well: Let's try to cut it:
        for (int i=(s.length()-1);i>1;i--)
        {
            Collection<String> tmp=findTupel(s.substring(0,i));
            if (tmp!=null)
            {
              if (strictMode && !isWord(s.substring(i))) {
                  continue;
                }
                tmp.add(s.substring(i));
                return tmp;
            }            
        }
        return null;
    }    

    /**
     * Removes e.g. 's' at the end of a string.
     */
    private String removeTailingCharacters(String str)
    {
        Collection<String> connChars = getConnectingCharacters();
        for (String connChar : connChars) {
          if (str.toUpperCase().endsWith(connChar.toUpperCase())) {
            return str.substring(0, str.length()-connChar.length());
          }
        }
        return str;
    }
    
    private Collection<String> findTupel(String s)
    {
        String right=s;
        String left="";
        if (s.length()<2)
          return null;
        Collection<String> result=new ArrayList<String>();
        
        for (int i=0;i<s.length();i++)
        {
            left=left+s.charAt(i);
            right=s.substring(left.length());
            String leftCleaned=removeTailingCharacters(left);
            boolean leftIsWord=false;
            if ((isWord(leftCleaned)))
            {
                if (hideConnectingCharacters)
                  result.add(leftCleaned);
               	else
                  result.add(left);
                leftIsWord=true;
            } else if ((isWord(left)))
            {
                result.add(left);
                leftIsWord=true;
            }
            if (leftIsWord) {
                //look if we can split the right part, too:
                Collection<String> rightCol=findTupel(right);
                if (rightCol!=null)
                  result.addAll(rightCol);
                else
                {
                        //we cannot split the rest of the word => left was not ok.
                        result=new ArrayList<String>();
                        continue;
                }
                return result;
            }
        }
        
        boolean stringIsWord=isWord(s);
        boolean cleanedStringIsWord=isWord(removeTailingCharacters(s));
        if (!stringIsWord && !cleanedStringIsWord) {
          return null;
        }
        if (hideConnectingCharacters && !stringIsWord) {
          result.add(removeTailingCharacters(s));
        } else {
          result.add(s);
        }
        return result;
    }
    
}