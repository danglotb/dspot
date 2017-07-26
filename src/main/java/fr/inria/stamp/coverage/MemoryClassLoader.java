package fr.inria.stamp.coverage;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class MemoryClassLoader extends URLClassLoader {

		private final Map<String, byte[]> definitions = new HashMap<>();

		public MemoryClassLoader(URL[] urls, ClassLoader classLoader) {
			super(urls, classLoader);
		}

		/**
		 * Add a in-memory representation of a class.
		 *
		 * @param name  name of the class
		 * @param bytes class definition
		 */
		public void addDefinition(final String name, final byte[] bytes) {
			definitions.put(name, bytes);
		}

		@Override
		protected Class<?> loadClass(final String name, final boolean resolve)
				throws ClassNotFoundException {
			final byte[] bytes = definitions.get(name);
			if (bytes != null) {
				return defineClass(name, bytes, 0, bytes.length);
			}
			return super.loadClass(name, resolve);
		}

	}