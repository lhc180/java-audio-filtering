package org.emmef.audio.servicemanager;

import java.net.URI;
import java.util.List;

import org.emmef.audio.format.AudioFormat;
import org.emmef.audio.nodes.SoundSink;
import org.emmef.audio.nodes.SoundSource;
import org.emmef.logging.Logger;
import org.emmef.servicemanager.AbstractServiceManager;
import org.emmef.servicemanager.ProviderVisitor;
import org.emmef.servicemanager.ProviderVisitor.VisitState;

public class SoundSourceAndSinkManager extends AbstractServiceManager<SoundSourceAndSinkProvider> implements SoundSourceAndSinkProvider {
	public static final Logger log = Logger.getDefault();
	public static final SoundSourceAndSinkManager instance = new SoundSourceAndSinkManager();
	
	public SoundSourceAndSinkManager() {
		super(SoundSourceAndSinkProvider.class, true);
	}
	
	public static final SoundSourceAndSinkManager getInstance() {
		return instance;
	}

	@Override
	public SoundSource createSource(URI sourceUri) {
		return visit(sourceUri, "", new Functor<String, SoundSource>() {
			@Override
			public SoundSource get(URI uri, SoundSourceAndSinkProvider soundSourceAndSinkProvider, String parameter) {
				return soundSourceAndSinkProvider.createSource(uri);
			}
		});
	}

	@Override
	public SoundSink createSink(URI sourceUri, AudioFormat format) {
		return visit(sourceUri, format, new Functor<AudioFormat, SoundSink>() {
			@Override
			public SoundSink get(URI uri, SoundSourceAndSinkProvider soundSourceAndSinkProvider, AudioFormat format) {
				return soundSourceAndSinkProvider.createSink(uri, format);
			}
		});
	}

	@Override
	public SoundSink createWithSameFormat(SoundSource source, URI targetUri) {
		return visit(targetUri, source, new Functor<SoundSource, SoundSink>() {
			@Override
			public SoundSink get(URI uri, SoundSourceAndSinkProvider soundSourceAndSinkProvider, SoundSource source) {
				return soundSourceAndSinkProvider.createWithSameFormat(source, uri);
			}
		});
	}
	
	public StringBuilder appendProviders(StringBuilder builder) {
		final StringBuilder text = builder != null ? builder : new StringBuilder();

		visit(new ProviderVisitor<SoundSourceAndSinkProvider>() {

			@Override
			public org.emmef.servicemanager.ProviderVisitor.VisitState visit(List<SoundSourceAndSinkProvider> list) {
				text.append(SoundSourceAndSinkManager.this.getClass().getSimpleName() + ": providers for interface " + getClassToProvide() + ":\n");
				for (SoundSourceAndSinkProvider provider : list) {
					text.append("- ").append(provider).append("\n");
				}
				return VisitState.SUCCESS;
			}});
		
		return text;
	}
	
	@SuppressWarnings("unchecked")
	private <T, V> V visit(final URI uri, final T argument, final Functor<T, V> functor) {
		final Object result[] = new Object[1];
		
		VisitState visitResult = visit(new ProviderVisitor<SoundSourceAndSinkProvider>() {

			@Override
			public ProviderVisitor.VisitState visit(List<SoundSourceAndSinkProvider> list) {
				for (SoundSourceAndSinkProvider provider : list) {
					try {
						result[0] = functor.get(uri, provider, argument);
					}
					catch (UnsupportedOperationException e) {
						log.debug(provider + ": " + e);
						continue;
					}
					catch (SoundException e) {
						log.debug(provider + ": " + e);
						continue;
					}
					if (result[0] != null) {
						return VisitState.SUCCESS;
					}
				}
				return VisitState.FAILURE;
			}});
		
		switch (visitResult) {
		case SUCCESS:
			return (V)result[0];
		case NONE:
			throw new IllegalStateException("Have no providers at all");
		default:
			throw new IllegalStateException("Have no provider for: uri=\"" + uri + "\"; argument=\"" + argument + "\"");
		}
	}
	
	private interface Functor<T, V> {
		V get(URI uri, SoundSourceAndSinkProvider soundSourceAndSinkProvider, T parameter);
	}
}