package dev.brighten.antivpn.utils.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class YamlConfiguration extends ConfigurationProvider
{

    private final ThreadLocal<Yaml> yaml = new ThreadLocal<Yaml>()
    {
        @Override
        protected Yaml initialValue()
        {

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle( DumperOptions.FlowStyle.BLOCK );

            return new Yaml(options);
        }
    };

    @Override
    public void save(Configuration config, File file) throws IOException
    {
        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( file ), StandardCharsets.UTF_8 ) )
        {
            save( config, writer );
        }
    }

    @Override
    public void save(Configuration config, Writer writer)
    {
        String contents = this.yaml.get().dump(config.self);

        List<String> list = new ArrayList<>();
        Collections.addAll(list, contents.split("\n"));

        int currentLayer = 0;
        StringBuilder currentPath = new StringBuilder();

        StringBuilder sb = new StringBuilder();

        int lineNumber = 0;
        for(Iterator<String> iterator = list.iterator(); iterator.hasNext(); lineNumber++) {
            String line = iterator.next();
            sb.append(line);
            sb.append('\n');

            if (!line.isEmpty()) {
                if (line.contains(":")) {

                    int layerFromLine = config.getLayerFromLine(line, lineNumber);

                    if (layerFromLine < currentLayer) {
                        currentPath = new StringBuilder(config.regressPathBy(currentLayer - layerFromLine, currentPath.toString()));
                    }

                    String key = config.getKeyFromLine(line);

                    if (currentLayer == 0) {
                        currentPath = new StringBuilder(key);
                    } else {
                        currentPath.append("." + key);
                    }

                    String path = currentPath.toString();
                    if (config.comments.containsKey(path)) {
                        config.comments.get(path).forEach(string -> {
                            sb.append(string);
                            sb.append('\n');
                        });
                    }
                }
            }
        }

        try {
            writer.write(contents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public Configuration load(File file) throws IOException
    {
        return load( file, null );
    }

    @Override
    public Configuration load(File file, Configuration defaults) throws IOException
    {
        try ( FileInputStream is = new FileInputStream( file ) )
        {
            return load( is, defaults );
        }
    }

    @Override
    public Configuration load(Reader reader)
    {
        return load( reader, null );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Configuration load(Reader reader, Configuration defaults)
    {
        Map<String, Object> map = yaml.get().loadAs( reader, LinkedHashMap.class );
        if ( map == null )
        {
            map = new LinkedHashMap<>();
        }
        return new Configuration( map, defaults );
    }

    @Override
    public Configuration load(InputStream is)
    {
        return load( is, null );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Configuration load(InputStream is, Configuration defaults)
    {
        Map<String, Object> map = yaml.get().loadAs( is, LinkedHashMap.class );
        if ( map == null )
        {
            map = new LinkedHashMap<>();
        }
        return new Configuration( map, defaults );
    }

    @Override
    public Configuration load(String string)
    {
        return load( string, null );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Configuration load(String string, Configuration defaults)
    {
        Map<String, Object> map = yaml.get().loadAs( string, LinkedHashMap.class );
        if ( map == null )
        {
            map = new LinkedHashMap<>();
        }
        return new Configuration( map, defaults );
    }
}